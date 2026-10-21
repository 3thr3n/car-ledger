package de.codeflowwizardry.carledger.map;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;

/**
 * The map of the GPS tracks: the browser loads the style and the tiles from
 * there, without a style URL the map isn't shown
 */
@ConfigMapping(prefix = "carledger.map")
public interface MapConfig
{
	/**
	 * @return URL of a MapLibre style (style.json), e.g. of an own tile server or
	 *         of a provider with its key; its origin is allowed in the CSP
	 */
	Optional<String> styleUrl();

	/**
	 * @return further origins the style loads from (tiles, glyphs, sprites on
	 *         other hosts), e.g. https://tiles.example.com, allowed in the CSP
	 */
	Optional<List<String>> sources();
}
