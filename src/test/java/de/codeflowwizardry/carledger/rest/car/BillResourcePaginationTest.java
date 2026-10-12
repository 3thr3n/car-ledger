package de.codeflowwizardry.carledger.rest.car;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.TestDataProfile;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
@TestProfile(TestDataProfile.class)
@TestHTTPEndpoint(BillResource.class)
class BillResourcePaginationTest
{
	@Test
	@TestSecurity(user = "alice", roles = "user")
	void shouldGetFirstPage()
	{
		given()
				.when()
				.pathParam("carId", 5L)
				.queryParam("page", 1)
				.queryParam("size", 5)
				.get("all")
				.then()
				.statusCode(200)
				.body("total", is(71))
				.body("data.size()", is(5));
	}

	@Test
	@TestSecurity(user = "alice", roles = "user")
	void shouldGetThirdPage()
	{
		given()
				.when()
				.pathParam("carId", 5L)
				.queryParam("page", 5)
				.queryParam("size", 15)
				.get("all")
				.then()
				.statusCode(200)
				.body("data.size()", is(11));
	}

	@Test
	@TestSecurity(user = "alice", roles = "user")
	void shouldGetOnlyYear2024()
	{
		given()
				.when()
				.pathParam("carId", 5L)
				.queryParam("page", 1)
				.queryParam("size", 25)
				.queryParam("year", 2024)
				.get("all")
				.then()
				.statusCode(200)
				.body("data.size()", is(15));
	}

	@Test
	@TestSecurity(user = "alice", roles = "user")
	void shouldNotAllowInvalidPage()
	{
		given()
				.when()
				.pathParam("carId", 5L)
				.queryParam("page", 0)
				.queryParam("size", 8)
				.get("all")
				.then()
				.statusCode(200)
				.body("data.size()", is(8));

		given()
				.when()
				.pathParam("carId", 5L)
				.queryParam("page", 10)
				.queryParam("size", 8)
				.get("all")
				.then()
				.statusCode(200)
				.body("data.size()", is(0));
	}

	@Test
	@TestSecurity(user = "alice", roles = "user")
	void shouldSortByDateDescendingByDefault()
	{
		List<String> dates = getAll("all", "date", null).jsonPath().getList("data.date", String.class);

		assertSorted(dates, 71, Comparator.reverseOrder());
	}

	@Test
	@TestSecurity(user = "alice", roles = "user")
	void shouldSortByTotal()
	{
		List<Float> ascending = getAll("all", "total", "ASC").jsonPath().getList("data.total", Float.class);
		List<Float> descending = getAll("all", "total", "DESC").jsonPath().getList("data.total", Float.class);

		assertSorted(ascending, 71, Comparator.naturalOrder());
		assertSorted(descending, 71, Comparator.reverseOrder());
	}

	@Test
	@TestSecurity(user = "alice", roles = "user")
	void shouldSortByTypeSpecificField()
	{
		List<Float> units = getAll("fuel/all", "unit", "ASC").jsonPath().getList("data.unit", Float.class);

		assertSorted(units, units.size(), Comparator.naturalOrder());
		assertEquals(true, units.size() > 1);
	}

	@Test
	@TestSecurity(user = "alice", roles = "user")
	void shouldRejectUnknownSortField()
	{
		given()
				.when()
				.pathParam("carId", 5L)
				.queryParam("sort", "unit")
				.get("all")
				.then()
				.statusCode(400);

		given()
				.when()
				.pathParam("carId", 5L)
				.queryParam("sort", "b.id; drop table bill")
				.get("all")
				.then()
				.statusCode(400)
				.body(containsString("Cannot sort by"));
	}

	private static io.restassured.response.Response getAll(String path, String sort, String order)
	{
		var request = given()
				.pathParam("carId", 5L)
				.queryParam("size", 100)
				.queryParam("sort", sort);
		if (order != null)
		{
			request.queryParam("order", order);
		}
		return request.when()
				.get(path)
				.then()
				.statusCode(200)
				.extract()
				.response();
	}

	private static <T> void assertSorted(List<T> values, int size, Comparator<? super T> comparator)
	{
		assertEquals(size, values.size());
		assertEquals(values.stream().sorted(comparator).toList(), values);
	}
}
