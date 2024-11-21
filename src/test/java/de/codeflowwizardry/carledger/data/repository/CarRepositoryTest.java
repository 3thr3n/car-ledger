package de.codeflowwizardry.carledger.data.repository;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CarRepositoryTest
{
	final CarRepository carRepository = new CarRepository();

	@Test
	void testFindByIdLong()
	{
		assertThrows(UnsupportedOperationException.class, () -> carRepository.findById(1L));
	}

	@Test
	void testFindByIdLongLockModeType()
	{
		assertThrows(UnsupportedOperationException.class, () -> carRepository.findById(1L, LockModeType.READ));
	}

	@Test
	void testFindByIdOptionalLong()
	{
		assertThrows(UnsupportedOperationException.class, () -> carRepository.findByIdOptional(1L));
	}

	@Test
	void testFindByIdOptionalLongLockModeType()
	{
		assertThrows(UnsupportedOperationException.class, () -> carRepository.findByIdOptional(1L, LockModeType.READ));
	}

}
