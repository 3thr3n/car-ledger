package de.codeflowwizardry.carledger.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AttachmentServiceTest
{
	@Test
	void shouldCleanTheFileName()
	{
		assertEquals("receipt.jpg", AttachmentService.cleanFileName("C:\\Users\\me\\receipt.jpg"));
		assertEquals("receipt.pdf", AttachmentService.cleanFileName("../../etc/receipt.pdf"));
		assertEquals("a b.png", AttachmentService.cleanFileName(" a\u0000 b.png\r\n"));
		assertEquals("attachment", AttachmentService.cleanFileName(null));
		assertEquals("attachment", AttachmentService.cleanFileName("dir/"));
		assertEquals(AttachmentService.MAX_FILE_NAME, AttachmentService.cleanFileName("x".repeat(300)).length());
	}
}
