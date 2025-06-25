package de.codeflowwizardry.carledger.rest.records.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fasterxml.jackson.annotation.JsonGetter;

public record AverageStats(BigDecimal pricePerUnit,BigDecimal distance,BigDecimal calculated,BigDecimal calculatedPrice){@JsonGetter("pricePerUnit")public String getPricePerUnit(){return pricePerUnit.setScale(1,RoundingMode.HALF_UP).toString();}

@JsonGetter("distance")public String getDistance(){return distance.setScale(2,RoundingMode.HALF_UP).toString();}

@JsonGetter("calculated")public String getCalculated(){return calculated.setScale(2,RoundingMode.HALF_UP).toString();}

@JsonGetter("calculatedPrice")public String getCalculatedPrice(){return calculatedPrice.setScale(2,RoundingMode.HALF_UP).toString();}}
