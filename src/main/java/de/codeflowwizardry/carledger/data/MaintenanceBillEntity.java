package de.codeflowwizardry.carledger.data;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "bill_maintenance")
public class MaintenanceBillEntity extends BillEntity
{
	@MapsId
	@OneToOne
	@JoinColumn(name = "id")
	private BillEntity bill;

    @Column(name = "m_description")
    private String description;
    @Column(name = "m_workshop")
    private String workshop;

    @Column(name = "m_labor_cost")
    private BigDecimal laborCost;
    @Column(name = "m_parts_cost")
    private BigDecimal partsCost;

	public MaintenanceBillEntity()
	{
	}

    public BillEntity getBill() {
        return bill;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWorkshop() {
        return workshop;
    }

    public void setWorkshop(String workshop) {
        this.workshop = workshop;
    }

    public BigDecimal getLaborCost() {
        return laborCost;
    }

    public void setLaborCost(BigDecimal laborCost) {
        this.laborCost = laborCost;
    }

    public BigDecimal getPartsCost() {
        return partsCost;
    }

    public void setPartsCost(BigDecimal partsCost) {
        this.partsCost = partsCost;
    }
}
