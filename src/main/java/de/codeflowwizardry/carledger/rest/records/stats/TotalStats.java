package de.codeflowwizardry.carledger.rest.records.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fasterxml.jackson.annotation.JsonGetter;

public record TotalStats(BigDecimal unit,BigDecimal distance,BigDecimal calculatedPrice){@JsonGetter("unit")public String getUnit(){return unit.setScale(2,RoundingMode.HALF_UP).toString();}

@JsonGetter("distance")public String getDistance(){return distance.setScale(2,RoundingMode.HALF_UP).toString();}

@JsonGetter("calculatedPrice")public String getCalculatedPrice(){return calculatedPrice.setScale(2,RoundingMode.HALF_UP).toString();}}
