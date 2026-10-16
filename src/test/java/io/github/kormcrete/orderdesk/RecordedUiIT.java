package io.github.kormcrete.orderdesk;

import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.RecordedTests;
import io.github.kormcrete.swingui.junit.UiTest;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

/** Replays every recorded test in {@code uitest/} against the running app. */
@UiTest
class RecordedUiIT {

    @TestFactory
    Stream<DynamicTest> recorded(Client ui) {
        return RecordedTests.of(ui);
    }
}
