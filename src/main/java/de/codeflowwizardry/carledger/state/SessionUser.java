package de.codeflowwizardry.carledger.state;

import java.util.List;

public record SessionUser(String username, String user, String email, List<String> roles) {
}
