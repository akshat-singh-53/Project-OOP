package com.itsm.strategy.priority;

import com.itsm.strategy.NamedStrategy;

/** Strategy pattern: interchangeable algorithms for turning incident attributes into a priority. */
public interface PriorityStrategy extends NamedStrategy {

    PriorityResult calculate(PriorityInput input);
}
