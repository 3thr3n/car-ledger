package de.codeflowwizardry.carledger.data;

/**
 * Needs to be extended from all <code>*BillEntity</code> Classes
 */
public abstract class AbstractBillEntity
{
	// Parent for different BillEntities
	public abstract BillEntity getBill();

	public abstract BillType getType();
}
