package org.ssglobal.revalida.ssclassroom.service.test;

import org.junit.jupiter.api.Test;
import org.ssglobal.revalida.ssclassroom.service.PrefixedId;
import org.ssglobal.revalida.ssclassroom.service.PrefixedIdGenerator;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class PrefixedIdGeneratorTest {

    static class Dummy {
        @PrefixedId(prefix = "crs_")
        String id;
    }

    @Test
    void generatesPrefixedUlid() throws Exception {
        Field field = Dummy.class.getDeclaredField("id");
        PrefixedId config = field.getAnnotation(PrefixedId.class);

        PrefixedIdGenerator generator = new PrefixedIdGenerator(config, field, null);

        String id = (String) generator.generate(null, null, null, null);
        System.out.println("Generated ID: " + id);

        assertTrue(id.startsWith("crs_"));
        assertEquals(30, id.length());
        assertTrue(id.substring(4).matches("[0-9A-HJKMNP-TV-Z]{26}"));
    }

    @Test
    void generatesDifferentIdsEachTime() throws Exception {
        Field field = Dummy.class.getDeclaredField("id");
        PrefixedId config = field.getAnnotation(PrefixedId.class);
        PrefixedIdGenerator generator = new PrefixedIdGenerator(config, field, null);

        String first = (String) generator.generate(null, null, null, null);
        String second = (String) generator.generate(null, null, null, null);

        assertNotEquals(first, second);
        assertTrue(first.compareTo(second) < 0); // monotonic: later IDs sort after earlier ones
    }
}