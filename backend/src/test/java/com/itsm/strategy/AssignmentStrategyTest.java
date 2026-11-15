package com.itsm.strategy;

import com.itsm.TestData;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.TechnicianLevel;
import com.itsm.strategy.assignment.LoadBasedAssignmentStrategy;
import com.itsm.strategy.assignment.RoundRobinAssignmentStrategy;
import com.itsm.strategy.assignment.SkillBasedAssignmentStrategy;
import com.itsm.strategy.assignment.SmartAssignmentStrategy;
import com.itsm.strategy.assignment.TechnicianCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentStrategyTest {

    private Incident dbIncident;
    private Technician dbExpert;
    private Technician networkGuy;
    private Technician generalist;
    private List<TechnicianCandidate> candidates;

    @BeforeEach
    void setUp() {
        Category database = TestData.category("DATABASE");
        Category network = TestData.category("NETWORK");
        var erp = TestData.service("ERP", 5, database);
        dbIncident = TestData.incident("ERP slow", "queries time out", erp, Impact.HIGH, Urgency.HIGH);
        dbIncident.classify(database, false);
        dbIncident.applyPriority(Priority.P1, 45, "test");

        dbExpert = TestData.technician("Priya", TechnicianLevel.L2, 6, 5, database);
        networkGuy = TestData.technician("Rahul", TechnicianLevel.TEAM_LEAD, 9, 5, network);
        generalist = TestData.technician("Arjun", TechnicianLevel.L1, 1, 5, network);
        candidates = List.of(
                new TechnicianCandidate(dbExpert, 4, 2),
                new TechnicianCandidate(networkGuy, 1, 0),
                new TechnicianCandidate(generalist, 0, 0));
    }

    @Test
    @DisplayName("TC12 Skill-based picks the technician skilled in the incident category")
    void skillBased() {
        assertThat(new SkillBasedAssignmentStrategy().select(dbIncident, candidates)).contains(dbExpert.getId());
    }

    @Test
    @DisplayName("TC13 Load-based picks the least busy technician")
    void loadBased() {
        assertThat(new LoadBasedAssignmentStrategy().select(dbIncident, candidates)).contains(generalist.getId());
    }

    @Test
    @DisplayName("TC14 Round robin rotates through technicians in turn")
    void roundRobin() {
        var strategy = new RoundRobinAssignmentStrategy();
        var first = strategy.select(dbIncident, candidates).orElseThrow();
        var second = strategy.select(dbIncident, candidates).orElseThrow();
        var third = strategy.select(dbIncident, candidates).orElseThrow();
        var fourth = strategy.select(dbIncident, candidates).orElseThrow();
        assertThat(List.of(first, second, third)).doesNotHaveDuplicates();
        assertThat(fourth).isEqualTo(first);
    }

    @Test
    @DisplayName("TC15 Smart strategy balances skill, workload and experience and explains its choice")
    void smart() {
        var ranking = new SmartAssignmentStrategy().rank(dbIncident, candidates);
        assertThat(ranking.get(0).technicianId()).isEqualTo(dbExpert.getId());
        assertThat(ranking.get(0).reasons()).anyMatch(r -> r.startsWith("Skill match"));
        assertThat(ranking.get(0).skillMatch()).isTrue();
    }

    @Test
    @DisplayName("TC16 No candidates means no selection (handled as TechnicianUnavailable upstream)")
    void emptyCandidates() {
        assertThat(new SmartAssignmentStrategy().select(dbIncident, List.of())).isEmpty();
        assertThat(new RoundRobinAssignmentStrategy().select(dbIncident, List.of())).isEmpty();
    }
}
