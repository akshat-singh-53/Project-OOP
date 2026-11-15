package com.itsm.config;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.sla.SlaPolicy;
import com.itsm.domain.sla.SlaViolation;
import com.itsm.domain.sla.SlaViolationType;
import com.itsm.domain.user.Role;
import com.itsm.domain.user.TechnicianLevel;
import com.itsm.domain.user.User;
import com.itsm.repository.AuditRecordRepository;
import com.itsm.repository.CategoryRepository;
import com.itsm.repository.ItServiceRepository;
import com.itsm.repository.SlaPolicyRepository;
import com.itsm.repository.SlaViolationRepository;
import com.itsm.repository.UserRepository;
import com.itsm.service.IncidentService;
import com.itsm.service.SettingsService;
import com.itsm.service.UserService;
import com.itsm.web.dto.IncidentDtos.CreateIncidentRequest;
import com.itsm.web.dto.UserDtos.CreateUserRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Loads reference data and a realistic incident history the first time the application starts. */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    public static final String DEMO_PASSWORD = "pass123";
    public static final String ADMIN_PASSWORD = "admin123";

    private final UserRepository users;
    private final CategoryRepository categories;
    private final ItServiceRepository services;
    private final SlaPolicyRepository policies;
    private final SlaViolationRepository violations;
    private final AuditRecordRepository audits;
    private final UserService userService;
    private final IncidentService incidentService;
    private final SettingsService settings;
    private final boolean enabled;
    private final boolean sampleIncidents;

    private final Map<String, Category> cat = new HashMap<>();
    private final Map<String, ItService> svc = new HashMap<>();
    private final Map<String, User> people = new HashMap<>();

    public DataSeeder(UserRepository users, CategoryRepository categories, ItServiceRepository services,
                      SlaPolicyRepository policies, SlaViolationRepository violations, AuditRecordRepository audits,
                      UserService userService, IncidentService incidentService, SettingsService settings,
                      @Value("${itsm.seed.enabled:true}") boolean enabled,
                      @Value("${itsm.seed.sample-incidents:true}") boolean sampleIncidents) {
        this.users = users;
        this.categories = categories;
        this.services = services;
        this.policies = policies;
        this.violations = violations;
        this.audits = audits;
        this.userService = userService;
        this.incidentService = incidentService;
        this.settings = settings;
        this.enabled = enabled;
        this.sampleIncidents = sampleIncidents;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || users.count() > 0) {
            return;
        }
        log.info("Empty database detected - loading demo data");
        seedCategories();
        seedServices();
        seedSlaPolicies();
        seedUsers();
        if (sampleIncidents) {
            seedIncidents();
        }
        log.info("Demo data loaded. Log in as admin/{} or any demo user with password {}", ADMIN_PASSWORD, DEMO_PASSWORD);
    }

    // ------------------------------------------------------------------ reference data

    private void seedCategories() {
        category("HARDWARE", "Hardware", "Laptops, desktops, printers and peripherals",
                "laptop, desktop, printer, monitor, keyboard, mouse, battery, hardware, device, screen, charger, flicker, scanner");
        category("SOFTWARE", "Software", "Desktop applications and licences",
                "software, install, update, license, office, excel, word, teams, outlook, crash, freeze, sync, attachment, "
                        + "audio, call, meeting, pdf, browser, application error, patch");
        category("NETWORK", "Network", "LAN, Wi-Fi, VPN and internet connectivity",
                "network, wifi, internet, vpn, lan, router, switch, dns, connectivity, disconnect, slow, latency, bandwidth, drop");
        category("DATABASE", "Database", "Database servers, queries and backups",
                "database, db, sql, query, oracle, mysql, postgres, deadlock, table, replication, backup, index, erp");
        category("CLOUD", "Cloud", "Cloud hosting, VMs, containers and deployments",
                "cloud, aws, azure, gcp, kubernetes, container, instance, vm, deployment, pod, cluster, server");
        category("SECURITY", "Security", "Malware, phishing and suspicious activity",
                "security, virus, malware, phishing, breach, attack, suspicious, ransomware, firewall, hack, spam, alert, "
                        + "pretend, fake, scam, spoof, unknown sender, another country");
        category("AUTHENTICATION", "Authentication", "Login, passwords, SSO and MFA",
                "login, password, authentication, sso, otp, mfa, locked, account, credential, token, expired, access denied");
        category("APPLICATION", "Application", "Business applications and customer-facing systems",
                "application, app, portal, payment, transaction, website, checkout, customer, 500, timeout, unavailable, page");
        category("STORAGE", "Storage", "File shares, disks and cloud storage",
                "storage, disk, drive, file, share, folder, nas, space, quota, onedrive, sharepoint, upload");
    }

    private void category(String code, String name, String description, String keywords) {
        cat.put(code, categories.save(new Category(code, name, description, keywords)));
    }

    private void seedServices() {
        service("Payment Gateway", "Online and in-store payment processing", 5, "APPLICATION", "Payments Platform");
        service("Customer Portal", "Public customer self-service website", 4, "APPLICATION", "Digital Channels");
        service("ERP Database", "Central Oracle database for finance and operations", 5, "DATABASE", "Data Platform");
        service("Single Sign-On", "Corporate identity provider (SSO / MFA)", 5, "AUTHENTICATION", "Identity & Access");
        service("Corporate Network", "Office LAN and Wi-Fi", 4, "NETWORK", "Network Operations");
        service("VPN Access", "Remote access for employees", 3, "NETWORK", "Network Operations");
        service("Email & Collaboration", "Email, Teams and calendars", 3, "SOFTWARE", "Workplace Services");
        service("Cloud Hosting", "Production workloads on the public cloud", 4, "CLOUD", "Cloud Operations");
        service("Cloud Storage", "File shares and document storage", 3, "STORAGE", "Workplace Services");
        service("Endpoint Security", "Antivirus, mail filtering and threat detection", 4, "SECURITY", "Security Operations");
        service("Office Hardware", "Laptops, desktops, printers and accessories", 2, "HARDWARE", "Desktop Support");
    }

    private void service(String name, String description, int criticality, String category, String team) {
        svc.put(name, services.save(new ItService(name, description, criticality, cat.get(category), team)));
    }

    private void seedSlaPolicies() {
        policies.save(new SlaPolicy("Critical (default)", Priority.P1, null, 5, 30, 75));
        policies.save(new SlaPolicy("High (default)", Priority.P2, null, 15, 120, 75));
        policies.save(new SlaPolicy("Medium (default)", Priority.P3, null, 60, 480, 75));
        policies.save(new SlaPolicy("Low (default)", Priority.P4, null, 240, 1440, 80));
        policies.save(new SlaPolicy("Payments critical (premium)", Priority.P1, svc.get("Payment Gateway"), 5, 20, 70));
    }

    private void seedUsers() {
        admin("admin", "Anita Desai", "anita.desai@itsm.local", "9810000001", true, ADMIN_PASSWORD);
        admin("sysadmin", "Rohan Mehta", "rohan.mehta@itsm.local", "9810000002", false, DEMO_PASSWORD);

        tech("rahul", "Rahul Verma", TechnicianLevel.TEAM_LEAD, 9, 6, "9820000001", "NETWORK", "CLOUD", "SECURITY");
        tech("priya", "Priya Nair", TechnicianLevel.L2, 6, 5, "9820000002", "DATABASE", "APPLICATION");
        tech("arjun", "Arjun Singh", TechnicianLevel.L1, 2, 5, "9820000003", "HARDWARE", "SOFTWARE");
        tech("sneha", "Sneha Iyer", TechnicianLevel.L2, 5, 5, "9820000004", "SECURITY", "AUTHENTICATION");
        tech("vikram", "Vikram Rao", TechnicianLevel.L1, 3, 4, "9820000005", "APPLICATION", "CLOUD", "STORAGE");
        tech("meera", "Meera Joshi", TechnicianLevel.L2, 4, 5, "9820000006", "NETWORK", "AUTHENTICATION", "SOFTWARE");

        employee("amit", "Amit Sharma", "Finance", "9830000001");
        employee("neha", "Neha Gupta", "Human Resources", "9830000002");
        employee("karan", "Karan Malhotra", "Sales", "9830000003");
        employee("isha", "Isha Kapoor", "Operations", "9830000004");
    }

    private void admin(String username, String name, String email, String phone, boolean manager, String password) {
        people.put(username, userService.create(new CreateUserRequest(Role.ADMIN, username, password, name, email,
                phone, null, null, null, null, null, manager), "SYSTEM"));
    }

    private void tech(String username, String name, TechnicianLevel level, int years, int max, String phone,
                      String... skills) {
        Set<Long> skillIds = new java.util.HashSet<>();
        for (String s : skills) {
            skillIds.add(cat.get(s).getId());
        }
        people.put(username, userService.create(new CreateUserRequest(Role.TECHNICIAN, username, DEMO_PASSWORD, name,
                username + "@itsm.local", phone, null, level, years, max, skillIds, null), "SYSTEM"));
    }

    private void employee(String username, String name, String department, String phone) {
        people.put(username, userService.create(new CreateUserRequest(Role.EMPLOYEE, username, DEMO_PASSWORD, name,
                username + "@company.local", phone, department, null, null, null, null, null), "SYSTEM"));
    }

    // ------------------------------------------------------------------ incident history

    private void seedIncidents() {
        // --- solved history (feeds reports, resolution suggestions and the k-NN classifier)
        closed(12, "amit", "VPN keeps disconnecting every few minutes",
                "Since the update my VPN disconnects every 5 minutes when working from home.", "VPN Access",
                Impact.MEDIUM, Urgency.HIGH, 15, 50,
                "Updated VPN client to v5.2 and switched the profile to TCP 443. Disconnects stopped.",
                "Outdated VPN client incompatible with new firewall rules", 5);
        closed(11, "neha", "Cannot login to SSO portal - password expired",
                "Login page says my password has expired and the reset link does not work.", "Single Sign-On",
                Impact.LOW, Urgency.HIGH, 1, 25,
                "Reset password from the identity admin console and cleared cached credentials in the browser.",
                "Password expiry policy; self-service reset link misconfigured", 4);
        closed(10, "karan", "Laptop battery drains within an hour",
                "My laptop battery goes from 100% to 0 in about an hour even when idle.", "Office Hardware",
                Impact.LOW, Urgency.LOW, 1, 600,
                "Battery health was 41%. Replaced battery under warranty.", "Battery wear", 5);
        closed(9, "isha", "ERP database queries extremely slow",
                "Month-end reports on the ERP take more than 5 minutes and often time out.", "ERP Database",
                Impact.HIGH, Urgency.HIGH, 120, 95,
                "Rebuilt fragmented indexes on the ORDERS table and refreshed statistics. Query time dropped from 40s to 1s.",
                "Index fragmentation after bulk data import", 4);
        closed(8, "amit", "Payment gateway timeout during checkout",
                "Customers see a timeout error at checkout and payments are not completing.", "Payment Gateway",
                Impact.CRITICAL, Urgency.CRITICAL, 2000, 45,
                "Restarted the payment service pods and increased the database connection pool from 20 to 80.",
                "Connection pool exhaustion under peak load", 4);
        closed(7, "neha", "Phishing email asking for credentials",
                "Several of us received an email pretending to be IT asking us to confirm our password.", "Endpoint Security",
                Impact.MEDIUM, Urgency.HIGH, 40, 70,
                "Blocked the sender domain at the mail gateway, purged the messages from all mailboxes and sent an awareness notice.",
                "Targeted phishing campaign", 5);
        closed(6, "karan", "Shared drive out of space, cannot save files",
                "Saving to the Sales shared folder fails with 'disk full'.", "Cloud Storage",
                Impact.MEDIUM, Urgency.HIGH, 25, 150,
                "Archived old project folders to cold storage and raised the share quota by 500 GB.",
                "Quota reached", 4);
        closed(5, "isha", "Wifi very slow on 3rd floor",
                "Wi-Fi on the 3rd floor is extremely slow and keeps dropping for everyone.", "Corporate Network",
                Impact.MEDIUM, Urgency.MEDIUM, 30, 300,
                "Replaced faulty access point AP-3F-02 and rebalanced Wi-Fi channels.", "Hardware fault in access point", 3);
        closed(4, "amit", "Customer portal showing 500 error on login page",
                "The customer portal login page returns HTTP 500 for all users.", "Customer Portal",
                Impact.HIGH, Urgency.CRITICAL, 500, 150,
                "Fixed misconfigured session cache endpoint and redeployed the portal.", "Bad configuration in release 3.4", 3);
        closed(3, "neha", "Printer on 2nd floor not printing",
                "Print jobs stay in the queue and nothing comes out of the 2nd floor printer.", "Office Hardware",
                Impact.LOW, Urgency.MEDIUM, 10, 200,
                "Cleared the print spooler and reinstalled the printer driver.", "Corrupted spooler", 5);
        closed(2, "karan", "MFA OTP not received on phone",
                "I am not receiving the one-time password SMS needed to log in.", "Single Sign-On",
                Impact.MEDIUM, Urgency.HIGH, 5, 60,
                "SMS provider route issue. Moved affected users to the authenticator app and raised a ticket with the vendor.",
                "Telecom route failure at SMS provider", 4);

        resolvedOnly(1, "isha", "Cloud VM instance unreachable after deployment",
                "After this morning's deployment the reporting VM cannot be reached on HTTPS.", "Cloud Hosting",
                Impact.HIGH, Urgency.HIGH, 50, 80,
                "Rolled back the security group change that blocked port 443.", "Firewall rule change");

        // --- open work in different lifecycle states
        open(Duration.ofHours(26), "karan", "Laptop screen flickering",
                "My laptop screen flickers constantly, especially when on battery.", "Office Hardware",
                Impact.LOW, Urgency.LOW, 1, i -> {
                    incidentService.acknowledge(i.getId(), i.getAssignedTechnician());
                    incidentService.startWork(i.getId(), i.getAssignedTechnician());
                    incidentService.addNote(i.getId(), "Updated graphics driver, monitoring the issue.", true,
                            i.getAssignedTechnician());
                });
        open(Duration.ofHours(5), "isha", "Database backup job failed last night",
                "The nightly ERP backup job shows status FAILED in the scheduler.", "ERP Database",
                Impact.MEDIUM, Urgency.MEDIUM, 1, i -> {
                    incidentService.acknowledge(i.getId(), i.getAssignedTechnician());
                    incidentService.startWork(i.getId(), i.getAssignedTechnician());
                    incidentService.requestInfo(i.getId(),
                            "Can you share the exact error message shown in the scheduler?", i.getAssignedTechnician());
                });
        open(Duration.ofMinutes(40), "neha", "VPN connection drops frequently when working from home",
                "My VPN disconnects every few minutes from home since yesterday.", "VPN Access",
                Impact.MEDIUM, Urgency.HIGH, 3, i -> incidentService.acknowledge(i.getId(), i.getAssignedTechnician()));
        open(Duration.ofMinutes(150), "amit", "Unable to access shared folder on cloud storage",
                "Access denied when opening the Finance folder on the file share.", "Cloud Storage",
                Impact.MEDIUM, Urgency.HIGH, 8, i -> {
                    incidentService.acknowledge(i.getId(), i.getAssignedTechnician());
                    incidentService.startWork(i.getId(), i.getAssignedTechnician());
                });
        open(Duration.ofMinutes(20), "karan", "Suspicious login alerts on my account",
                "I received alerts about logins from another country that were not me.", "Single Sign-On",
                Impact.HIGH, Urgency.HIGH, 1, i -> { });

        // --- a burst of related payment incidents: duplicate detection, correlation and escalation demo
        open(Duration.ofMinutes(9), "amit", "Payment application unavailable",
                "The payment application is down and users are unable to complete transactions.", "Payment Gateway",
                Impact.CRITICAL, Urgency.CRITICAL, 1500, i -> { });
        open(Duration.ofMinutes(6), "isha", "Payment transaction failing",
                "Payment transactions are failing for customers at checkout.", "Payment Gateway",
                Impact.HIGH, Urgency.CRITICAL, 300, i -> { });

        settings.set(SettingsService.AUTO_ASSIGN, "false");
        open(Duration.ofMinutes(3), "neha", "Unable to make payment from customer app",
                "Customers report they are unable to make a payment from the mobile app.", "Payment Gateway",
                Impact.HIGH, Urgency.HIGH, 200, i -> { });
        open(Duration.ofMinutes(2), "karan", "Teams calls dropping audio",
                "Audio drops every few minutes during Teams calls.", "Email & Collaboration",
                Impact.LOW, Urgency.MEDIUM, 4, i -> { });
        settings.set(SettingsService.AUTO_ASSIGN, "true");
    }

    private Incident report(String reporter, String title, String description, String service, Impact impact,
                            Urgency urgency, int users) {
        return incidentService.create(new CreateIncidentRequest(title, description, svc.get(service).getId(), null,
                impact, urgency, users), people.get(reporter)).incident();
    }

    private void closed(int daysAgo, String reporter, String title, String description, String service,
                        Impact impact, Urgency urgency, int users, int minutesToResolve, String resolution,
                        String rootCause, int rating) {
        Incident i = report(reporter, title, description, service, impact, urgency, users);
        User tech = i.getAssignedTechnician();
        User owner = people.get(reporter);
        shift(i, Duration.ofMinutes(Math.max(1, minutesToResolve / 10)));
        incidentService.acknowledge(i.getId(), tech);
        incidentService.startWork(i.getId(), tech);
        shift(i, Duration.ofMinutes(minutesToResolve - minutesToResolve / 10));
        incidentService.resolve(i.getId(), resolution, rootCause, tech);
        reconcileSla(i);
        shift(i, Duration.ofMinutes(45));
        incidentService.verify(i.getId(), owner);
        incidentService.feedback(i.getId(), rating, rating >= 4 ? "Quick and helpful, thanks!" : "Fixed, but took a while.", owner);
        shift(i, Duration.ofMinutes(30));
        incidentService.close(i.getId(), "Verified by reporter", tech);
        shift(i, Duration.ofDays(daysAgo).minusMinutes(minutesToResolve + 75));
    }

    private void resolvedOnly(int daysAgo, String reporter, String title, String description, String service,
                              Impact impact, Urgency urgency, int users, int minutesToResolve, String resolution,
                              String rootCause) {
        Incident i = report(reporter, title, description, service, impact, urgency, users);
        User tech = i.getAssignedTechnician();
        shift(i, Duration.ofMinutes(5));
        incidentService.acknowledge(i.getId(), tech);
        incidentService.startWork(i.getId(), tech);
        shift(i, Duration.ofMinutes(minutesToResolve - 5));
        incidentService.resolve(i.getId(), resolution, rootCause, tech);
        reconcileSla(i);
        shift(i, Duration.ofHours(daysAgo * 20L));
    }

    private void open(Duration age, String reporter, String title, String description, String service,
                      Impact impact, Urgency urgency, int users, Consumer<Incident> progress) {
        Incident i = report(reporter, title, description, service, impact, urgency, users);
        progress.accept(i);
        shift(i, age);
    }

    /** Moves the incident and its audit entries back in time to simulate history. */
    private void shift(Incident incident, Duration back) {
        if (back.isZero() || back.isNegative()) {
            return;
        }
        incident.shiftTimeline(back);
        audits.findByIncidentCodeOrderByTimestampAscIdAsc(incident.getCode()).forEach(a -> a.shiftTimeline(back));
    }

    private void reconcileSla(Incident i) {
        if (i.getResolvedAt().isAfter(i.getResolutionDueAt())) {
            i.markResolutionBreached();
            violations.save(new SlaViolation(i, SlaViolationType.RESOLUTION, i.getResolutionDueAt(), i.getResolvedAt()));
        }
    }
}
