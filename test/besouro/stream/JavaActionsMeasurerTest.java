package besouro.stream;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.runtime.IPath;
import org.junit.Before;
import org.junit.Test;

import besouro.listeners.JavaStatementMeter;
import besouro.listeners.mock.ResourceChangeEventFactory;
import besouro.model.Action;
import besouro.model.EditAction;
import besouro.model.FileOpenedAction;



public class JavaActionsMeasurerTest {
	
	private IResource file;
	private JavaActionsMeasurer stream;
	private EditAction action1;
	private EditAction action2;
	private Date clock;
	
	private JavaStatementMeter metric;

	@Before
	public void setup() throws Exception {

		stream = new JavaActionsMeasurer();

		// strange, i know
		JavaStatementMeter measurer = mock(JavaStatementMeter.class);
		metric = mock(JavaStatementMeter.class);
		when(measurer.measureJavaFile(any(IFile.class))).thenReturn(metric);
		stream.setJavaFileMeasurer(measurer);

		
		file = mock(IFile.class);
		IPath path = mock(IPath.class);
		File aFile = mock(File.class);
		when(file.getName()).thenReturn("afile.any");
		when(file.getLocation()).thenReturn(path);
		when(path.toFile()).thenReturn(aFile);
		when(aFile.length()).thenReturn(33l);
		
		clock = new Date();
		action1 = new EditAction(clock, file);
		action2 = new EditAction(clock, file);
		action2.setPreviousAction(action1);
		

	}
	
	
	
	@Test
	public void shouldLinkTheEditActions() throws Exception {
		
		// two brand new *unlinked* actions
		action1 = new EditAction(clock, file);
		action2 = new EditAction(clock, file);
		
		stream.measureJavaActions(action1);
		stream.measureJavaActions(action2);
		
		Assert.assertEquals(action1, action2.getPreviousAction());
		
	}
	
	
	@Test
	public void shouldLinkActionsPerFile() throws Exception {
		
		IResource anotherFile = ResourceChangeEventFactory.createMockResource("anotherfile.any", 33);
//		when(anotherFile.getName()).thenReturn("anotherfile.any");
		
		action1 = new EditAction(clock, file);
		action2 = new EditAction(clock, file);
		EditAction action3 = new EditAction(clock, anotherFile);
		EditAction action4 = new EditAction(clock, anotherFile);

		stream.measureJavaActions(action1);
		stream.measureJavaActions(action3);
		stream.measureJavaActions(action2);
		stream.measureJavaActions(action4);
		
		// should link action1 -> action2
		Assert.assertEquals(action1, action2.getPreviousAction());
		Assert.assertNull(action1.getPreviousAction());
		
		// should link action3 -> action4
		Assert.assertEquals(action3, action4.getPreviousAction());
		Assert.assertNull(action3.getPreviousAction());
	}
	
	
	
	
	
	@Test
	public void shouldCalculateFileIncreases() throws Exception {
		action1.setFileSize(50);
		action2.setFileSize(150);
		Assert.assertEquals(0, action1.getFileSizeIncrease());
		Assert.assertEquals(100, action2.getFileSizeIncrease());
	}
	
	@Test
	public void shouldCalculateMethodsIncreases() throws Exception {
		action1.setMethodsCount(5);
		action2.setMethodsCount(7);
		Assert.assertEquals(0, action1.getMethodIncrease());
		Assert.assertEquals(2, action2.getMethodIncrease());
	}

	@Test
	public void shouldCalculateStatementsIncreases() throws Exception {
		action1.setStatementsCount(5);
		action2.setStatementsCount(8);
		Assert.assertEquals(0, action1.getStatementIncrease());
		Assert.assertEquals(3, action2.getStatementIncrease());
	}
	
	@Test
	public void shouldCalculateTestAssertionsIncreases() throws Exception {
		action1.setTestAssertionsCount(15);
		action2.setTestAssertionsCount(19);
		Assert.assertEquals(0, action1.getTestAssertionIncrease());
		Assert.assertEquals(4, action2.getTestAssertionIncrease());
	}
	
	@Test
	public void shouldCalculateTestMethodsIncreases() throws Exception {
		action1.setTestMethodsCount(11);
		action2.setTestMethodsCount(19);
		Assert.assertEquals(0, action1.getTestMethodIncrease());
		Assert.assertEquals(8, action2.getTestMethodIncrease());
	}

	
	
	@Test
	public void shouldLinkEditActionsWithFileOpenActions() throws Exception {
		
		FileOpenedAction open = new FileOpenedAction(clock, file);
		
		// two brand new *unlinked* actions
		action1 = new EditAction(clock, file);
		action2 = new EditAction(clock, file);
		
		IResource anotherFile = ResourceChangeEventFactory.createMockResource("anotherfile.any", 33);
//		when(anotherFile.getName()).thenReturn("anotherfile.any");

		
		EditAction action3 = new EditAction(clock, anotherFile);

		stream.measureJavaActions(open);
		stream.measureJavaActions(action1);
		stream.measureJavaActions(action3); // should not be linked
		stream.measureJavaActions(action2);
		
		Assert.assertEquals(open, action1.getPreviousAction());
		Assert.assertEquals(action1, action2.getPreviousAction());
		
		Assert.assertNull(action3.getPreviousAction());
		
	}
	
	@Test
	public void shouldCalculateFileIncreasesToPreviousFileOpenAction() throws Exception {
		
		FileOpenedAction open = new FileOpenedAction(clock, file);
		open.setFileSize(50);
		
		action2 = new EditAction(clock, file);
		action2.setFileSize(150);
		
		action2.setPreviousAction(open);
		
		Assert.assertEquals(100, action2.getFileSizeIncrease());
	}


	@Test
	public void shouldRecognizeTestEditsByNumberOfTestsAndAsserts() throws Exception {
		
		// its how test edits are identified 
		when(metric .isTest()).thenReturn(Boolean.TRUE);
		when(metric.getNumOfMethods()       ).thenReturn(1);
		when(metric.getNumOfStatements()    ).thenReturn(2);
		when(metric.getNumOfTestAssertions()).thenReturn(3);
		when(metric.getNumOfTestMethods()   ).thenReturn(4);
		
		stream.measureJavaActions(action1);
		
		Assert.assertEquals("afile.any", action1.getResource().getName());
		
		Assert.assertEquals(true, action1.isTestEdit());
		Assert.assertEquals(1, action1.getMethodsCount());
		Assert.assertEquals(2, action1.getStatementsCount());
		Assert.assertEquals(3, action1.getTestAssertionsCount());
		Assert.assertEquals(4, action1.getTestMethodsCount());
		
		Assert.assertEquals(33, action1.getFileSize());
		
	}

	@Test
	public void shouldRecognizeProductionEdits() throws Exception {
		
		// it depends on the implementation of JavaStatementMeter (is not being testet yet)
		when(metric.isTest()).thenReturn(Boolean.FALSE);
		
		stream.measureJavaActions(action1);

		Assert.assertEquals(false, action1.isTestEdit());

	}

	
	@Test
	public void shouldRecognizeTestEditsByTesInTheNameOfTheClass() throws Exception {
		
		// it depends on the implementation of JavaStatementMeter (is not being testet yet)
		when(metric.isTest()).thenReturn(Boolean.TRUE);
		
		stream.measureJavaActions(action1);
		
		Assert.assertEquals(true, action1.isTestEdit());
		
	}
	
}