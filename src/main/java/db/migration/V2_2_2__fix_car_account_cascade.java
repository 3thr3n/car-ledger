package db.migration;

import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2_2_2__fix_car_account_cascade extends BaseJavaMigration
{
	@Override
	public void migrate(Context context) throws Exception
	{
		try (Statement stmt = context.getConnection().createStatement())
		{
			String constraintName;

			try (ResultSet rs = stmt.executeQuery(
					"SELECT constraint_name FROM information_schema.table_constraints " +
							"WHERE table_name = 'car' AND constraint_type = 'FOREIGN KEY'"))
			{
				rs.next();
				constraintName = rs.getString("constraint_name");
			}

			stmt.execute("ALTER TABLE car DROP CONSTRAINT " + constraintName);
			stmt.execute("ALTER TABLE car ADD CONSTRAINT car_user_id_fkey " +
					"FOREIGN KEY (user_id) REFERENCES account (id) ON DELETE CASCADE");
		}

	}
}
