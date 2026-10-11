package de.codeflowwizardry.carledger;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/**
 * Counts the SQL statements executed by an action, needs
 * <code>quarkus.hibernate-orm.statistics</code>.
 */
public final class QueryCounter
{
	private QueryCounter()
	{
	}

	public static long count(SessionFactory sessionFactory, Runnable action)
	{
		Statistics statistics = sessionFactory.getStatistics();
		statistics.clear();
		action.run();
		return statistics.getPrepareStatementCount();
	}
}
