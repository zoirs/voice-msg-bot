package ru.chernyshev.recognizer.utils;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class EnvUtilsTest {

    @Test
    public void setEnvTest() throws Exception {
        String variable = "some.variable";
        String value = "some.value";

        assertNull(System.getenv().get(variable));
        EnvUtils.setEnv(ImmutableMap.of(variable, value));
        assertThat(System.getenv().get(variable), is(value));
    }
}
