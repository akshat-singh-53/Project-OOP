package com.itsm.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsm.domain.incident.Incident;
import com.itsm.repository.AuditRecordRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.ItServiceRepository;
import com.itsm.repository.NotificationRepository;
import com.itsm.service.SlaMonitorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end tests through HTTP with the real Spring context and an in-memory database. */
@SpringBootTest
@AutoConfigureMockMvc
class IncidentApiIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private ItServiceRepository services;
    @Autowired
    private AuditRecordRepository audits;
    @Autowired
    private NotificationRepository notifications;
    @Autowired
    private IncidentRepository incidentRepository;
    @Autowired
    private SlaMonitorService monitor;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    private JsonNode createIncident(String token, String title, String description, String service) throws Exception {
        Long serviceId = services.findByNameIgnoreCase(service).orElseThrow().getId();
        String body = mvc.perform(post("/api/incidents").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", title, "description", description,
                                "serviceId", serviceId, "impact", "CRITICAL", "urgency", "CRITICAL",
                                "affectedUsers", 1000))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    @Test
    @DisplayName("TC25 Invalid login is rejected with 401 INVALID_LOGIN")
    void invalidLogin() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"amit\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_LOGIN"));
    }

    @Test
    @DisplayName("TC26 Requests without a token get 401; employees get 403 on admin endpoints")
    void authorisation() throws Exception {
        mvc.perform(get("/api/incidents")).andExpect(status().isUnauthorized());
        String employee = login("amit", "pass123");
        mvc.perform(get("/api/users").header("Authorization", employee))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_OPERATION"));
        mvc.perform(get("/api/settings").header("Authorization", employee)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC27 Invalid incident input returns 400 with field errors")
    void validation() throws Exception {
        String employee = login("neha", "pass123");
        mvc.perform(post("/api/incidents").header("Authorization", employee).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"description\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    @DisplayName("TC28 Workflow: employee reports -> auto classify -> P1 -> SLA -> technician assigned -> audited & notified")
    void creationPipeline() throws Exception {
        long notificationsBefore = notifications.count();
        String employee = login("karan", "pass123");
        JsonNode result = createIncident(employee, "Payment application unavailable",
                "The payment application is down and users are unable to complete transactions.", "Payment Gateway");

        JsonNode incident = result.get("incident");
        JsonNode summary = incident.get("summary");
        assertThat(summary.get("priority").asText()).isEqualTo("P1");
        assertThat(summary.get("category").get("code").asText()).isEqualTo("APPLICATION");
        assertThat(incident.get("categorySuggestedAutomatically").asBoolean()).isTrue();
        assertThat(summary.get("status").asText()).isEqualTo("ASSIGNED");
        assertThat(summary.get("technician")).isNotNull();
        assertThat(incident.get("resolutionTargetMinutes").asInt()).isEqualTo(20); // service-specific SLA override

        String code = summary.get("code").asText();
        assertThat(audits.findByIncidentCodeOrderByTimestampAscIdAsc(code))
                .extracting(a -> a.getAction())
                .contains("CREATED", "CLASSIFIED", "PRIORITY_CALCULATED", "SLA_APPLIED", "ASSIGNED");
        assertThat(notifications.count()).isGreaterThan(notificationsBefore);
    }

    @Test
    @DisplayName("TC29 Invalid lifecycle transition through the API returns 409; duplicate re-submission is blocked")
    void invalidTransitionAndResubmission() throws Exception {
        String employee = login("isha", "pass123");
        JsonNode result = createIncident(employee, "Customer portal not loading",
                "The customer portal page shows a blank screen for everyone.", "Customer Portal");
        long id = result.get("incident").get("summary").get("id").asLong();

        String admin = login("admin", "admin123");
        mvc.perform(post("/api/incidents/" + id + "/resolve").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolution\":\"Restarted the portal servers\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

        Long serviceId = services.findByNameIgnoreCase("Customer Portal").orElseThrow().getId();
        mvc.perform(post("/api/incidents").header("Authorization", employee).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Customer portal not loading",
                                "description", "Same problem submitted twice by mistake", "serviceId", serviceId,
                                "impact", "LOW", "urgency", "LOW"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_INCIDENT"));
    }

    @Test
    @DisplayName("TC32 Scheduled SLA monitor saves its results: a violation is reported once, not on every run")
    void scheduledMonitorDoesNotRepeat() throws Exception {
        JsonNode result = createIncident(login("neha", "pass123"), "Cloud hosting cluster down",
                "All virtual machines in the production cloud cluster are unreachable.", "Cloud Hosting");
        long id = result.get("incident").get("summary").get("id").asLong();
        String code = result.get("incident").get("summary").get("code").asText();
        new TransactionTemplate(transactionManager).executeWithoutResult(s ->
                incidentRepository.findById(id).orElseThrow().shiftTimeline(Duration.ofDays(3)));

        monitor.scheduledRun();
        monitor.scheduledRun();

        assertThat(audits.findByIncidentCodeOrderByTimestampAscIdAsc(code))
                .filteredOn(a -> "SLA_VIOLATED".equals(a.getAction()))
                .hasSize(2); // one response and one resolution violation
        Incident incident = incidentRepository.findById(id).orElseThrow();
        assertThat(incident.isResponseBreached()).isTrue();
        assertThat(incident.isResolutionBreached()).isTrue();
        assertThat(incident.getEscalationLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC30 Employees cannot see incidents reported by someone else")
    void employeeIsolation() throws Exception {
        JsonNode result = createIncident(login("amit", "pass123"), "ERP database unreachable",
                "The ERP database refuses all connections since 9am.", "ERP Database");
        long id = result.get("incident").get("summary").get("id").asLong();
        mvc.perform(get("/api/incidents/" + id).header("Authorization", login("neha", "pass123")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/incidents/" + id).header("Authorization", login("admin", "admin123")))
                .andExpect(status().isOk());
    }
}
