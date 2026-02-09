package de.codeflowwizardry.carledger.csv;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CsvColumnMapping
{
	private final Map<String, Integer> columnIndexes;

	public CsvColumnMapping(Map<String, Integer> columnIndexes)
	{
		this.columnIndexes = new HashMap<>(columnIndexes);
	}

	public Integer get(String fieldName)
	{
		return columnIndexes.get(fieldName);
	}

	public boolean has(String fieldName)
	{
		return columnIndexes.containsKey(fieldName);
	}

	// Validation method
	public void validateRequired(Set<String> requiredFields)
	{
		Set<String> missingFields = new HashSet<>();

		for (String requiredField : requiredFields)
		{
			if (!has(requiredField))
			{
				missingFields.add(requiredField);
			}
		}

		if (!missingFields.isEmpty())
		{
			throw new IllegalArgumentException(
					"Missing required field mappings: " + String.join(", ", missingFields));
		}
	}

	public static CsvColumnMapping fromJson(String json) throws JsonProcessingException
	{
		if (StringUtils.isBlank(json))
		{
			throw new IllegalArgumentException("CSV column mapping cannot be empty!");
		}

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Integer> map = mapper.readValue(json,
				new TypeReference<>() {
				});
		return new CsvColumnMapping(map);
	}
}
