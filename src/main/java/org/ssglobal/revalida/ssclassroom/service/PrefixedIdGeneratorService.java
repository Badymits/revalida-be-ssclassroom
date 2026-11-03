package org.ssglobal.revalida.ssclassroom.service;

import com.github.f4b6a3.ulid.UlidCreator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;

import java.lang.reflect.Member;
import java.util.EnumSet;

public class PrefixedIdGeneratorService implements BeforeExecutionGenerator {

    private final String prefix;

    public PrefixedIdGeneratorService(PrefixedIdService config, Member annotatedMember,
                               GeneratorCreationContext context) {
        this.prefix = config.prefix();
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner,
                           Object currentValue, EventType eventType) {
        return prefix + UlidCreator.getMonotonicUlid().toString();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EventTypeSets.INSERT_ONLY;
    }
}