package de.codeflowwizardry.carledger.rest.converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

@Provider
public class EnumParamConverterProvider implements ParamConverterProvider
{
	@Override
	@SuppressWarnings("unchecked")
	public <T> ParamConverter<T> getConverter(Class<T> rawType,
			Type genericType,
			Annotation[] annotations)
	{
		if (rawType.isEnum())
		{
			return (ParamConverter<T>) new EnumParamConverter<>((Class<Enum>) rawType);
		}
		return null;
	}
}
