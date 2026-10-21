package io.github.kormcrete.orderdesk;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

class NoticesTest {

    @Test
    void thirdPartyNoticesNameEveryBundledLibrary() {
        InputStream in = getClass().getClassLoader().getResourceAsStream("META-INF/THIRD-PARTY-NOTICES.txt");
        assertThat(in).isNotNull();
        String text = new Scanner(in, "UTF-8").useDelimiter("\\A").next();
        assertThat(text).contains("FlatLaf", "JSVG", "Jannis Weis", "MigLayout", "Mikael Grev", "Lucide", "Cole Bemis",
                "swing-flatlaf-lagoon-theme", "Copyright 2026 kormcrete and the swing-flatlaf-lagoon-theme contributors");
    }
}
