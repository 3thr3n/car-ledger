package de.codeflowwizardry.carledger.state;

import org.infinispan.protostream.annotations.Proto;

import java.io.Serializable;

@Proto
public class SessionToken implements Serializable
{
	String accessToken;
	String refreshToken;

	public SessionToken()
	{
		this.accessToken = null;
		this.refreshToken = null;
	}

	public SessionToken(String accessToken, String refreshToken)
	{
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}

	public String getAccessToken()
	{
		return accessToken;
	}

	public void setAccessToken(String accessToken)
	{
		this.accessToken = accessToken;
	}

	public String getRefreshToken()
	{
		return refreshToken;
	}
}
