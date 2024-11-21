package de.codeflowwizardry.carledger.state;

import org.infinispan.protostream.annotations.Proto;

@Proto
public record AuthTokenState(String accessToken, String idToken, String refreshToken, long expiresIn, String id)
{
}
