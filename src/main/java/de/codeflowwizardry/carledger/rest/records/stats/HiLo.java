package de.codeflowwizardry.carledger.rest.records.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public record HiLo(BigDecimal min,BigDecimal max,int scale){@JsonGetter("min")public String getMin(){return min.setScale(scale,RoundingMode.HALF_UP).toString();}

@JsonGetter("max")public String getMax(){return max.setScale(scale,RoundingMode.HALF_UP).toString();}

@Override @JsonIgnore @SuppressWarnings("java:S6207")public int scale(){return scale;}}
