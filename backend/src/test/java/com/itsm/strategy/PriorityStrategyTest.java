package com.itsm.strategy;

import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.incident.Urgency;
import com.itsm.strategy.priority.MatrixPriorityStrategy;
import com.itsm.strategy.priority.PriorityInput;
import com.itsm.strategy.priority.PriorityStrategy;
import com.itsm.strategy.priority.WeightedPriorityStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityStrategyTest {

    private final PriorityStrategy matrix = new MatrixPriorityStrategy();
    private final PriorityStrategy weighted = new WeightedPriorityStrategy();

    @Test
    @DisplayName("TC01 Payment outage (high impact, critical urgency, criticality 5) is P1")
    void paymentOutageIsCritical() {
        var result = matrix.calculate(new PriorityInput(Impact.HIGH, Urgency.CRITICAL, 5, 2000));
        assertThat(result.priority()).isEqualTo(Priority.P1);
        assertThat(result.score()).isEqualTo(60);
        assertThat(result.explanation()).contains("= 60");
    }

    @Test
    @DisplayName("TC02 Single-user low-urgency problem on a minor service is P4")
    void minorIssueIsLow() {
        var result = matrix.calculate(new PriorityInput(Impact.LOW, Urgency.MEDIUM, 2, 1));
        assertThat(result.priority()).isEqualTo(Priority.P4);
    }

    @ParameterizedTest(name = "TC03 boundary: {0} x {1} x {2} -> {3}")
    @CsvSource({
            "MEDIUM, CRITICAL, 5, P1",   // 2*4*5 = 40, exactly the P1 threshold
            "HIGH,   HIGH,     4, P2",   // 36
            "MEDIUM, MEDIUM,   5, P2",   // 20, exactly the P2 threshold
            "MEDIUM, MEDIUM,   2, P3",   // 8, exactly the P3 threshold
            "LOW,    LOW,      5, P4"    // 5
    })
    void matrixBoundaries(Impact impact, Urgency urgency, int criticality, Priority expected) {
        assertThat(matrix.calculate(new PriorityInput(impact, urgency, criticality, 1)).priority()).isEqualTo(expected);
    }

    @Test
    @DisplayName("TC04 Weighted strategy raises priority when many users are affected")
    void weightedConsidersAffectedUsers() {
        var few = weighted.calculate(new PriorityInput(Impact.MEDIUM, Urgency.HIGH, 3, 1));
        var many = weighted.calculate(new PriorityInput(Impact.MEDIUM, Urgency.HIGH, 3, 5000));
        assertThat(many.score()).isGreaterThan(few.score());
        assertThat(many.priority().isHigherThan(few.priority()) || many.priority() == few.priority()).isTrue();
        assertThat(weighted.calculate(new PriorityInput(Impact.CRITICAL, Urgency.CRITICAL, 5, 10000)).priority())
                .isEqualTo(Priority.P1);
    }
}
