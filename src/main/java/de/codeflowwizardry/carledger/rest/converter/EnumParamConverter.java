package de.codeflowwizardry.carledger.rest.converter;

import jakarta.ws.rs.ext.ParamConverter;

public class EnumParamConverter<T extends Enum<T>> implements ParamConverter<T>
{
	private final Class<T> enumClass;

	public EnumParamConverter(Class<T> enumClass)
	{
		this.enumClass = enumClass;
	}

	@Override
	public T fromString(String value)
	{
		if (value == null || value.isBlank())
		{
			return null;
		}
		try
		{
			return Enum.valueOf(enumClass, value.toUpperCase());
		}
		catch (IllegalArgumentException e)
		{
			// Try case-insensitive matching
			for (T enumConstant : enumClass.getEnumConstants())
			{
				if (enumConstant.name().equalsIgnoreCase(value))
				{
					return enumConstant;
				}
			}
			throw new IllegalArgumentException(
					String.format("Invalid %s: %s", enumClass.getSimpleName(), value));
		}
	}

	@Override
	public String toString(T value)
	{
		return value != null ? value.name() : null;
	}
}
