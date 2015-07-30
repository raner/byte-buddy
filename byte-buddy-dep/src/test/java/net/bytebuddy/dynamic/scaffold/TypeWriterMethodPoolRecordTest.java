package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.ModifierResolver;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeWriterMethodPoolRecordTest {

    private static final int MODIFIERS = 42, ONE = 1, TWO = 2, MULTIPLIER = 4;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodAttributeAppender methodAttributeAppender;

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private AnnotationVisitor annotationVisitor;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private ByteCodeAppender byteCodeAppender, otherAppender;

    @Mock
    private GenericTypeList exceptionTypes;

    @Mock
    private TypeList rawExceptionTypes;

    @Mock
    private ParameterDescription parameterDescription;

    @Mock
    private ModifierResolver modifierResolver;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getDescriptor()).thenReturn(BAR);
        when(methodDescription.getGenericSignature()).thenReturn(QUX);
        when(methodDescription.getExceptionTypes()).thenReturn(exceptionTypes);
        when(modifierResolver.transform(eq(methodDescription), anyBoolean())).thenReturn(MODIFIERS);
        when(methodDescription.getAdjustedModifiers(anyBoolean())).thenReturn(MODIFIERS);
        when(exceptionTypes.asRawTypes()).thenReturn(rawExceptionTypes);
        when(rawExceptionTypes.toInternalNames()).thenReturn(new String[]{BAZ});
        when(classVisitor.visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ})).thenReturn(methodVisitor);
        when(methodDescription.getParameters())
                .thenReturn((ParameterList) new ParameterList.Explicit<ParameterDescription>(Collections.singletonList(parameterDescription)));
        when(parameterDescription.getName()).thenReturn(FOO);
        when(parameterDescription.getModifiers()).thenReturn(MODIFIERS);
        when(methodVisitor.visitAnnotationDefault()).thenReturn(annotationVisitor);
        when(byteCodeAppender.apply(methodVisitor, implementationContext, methodDescription))
                .thenReturn(new ByteCodeAppender.Size(ONE, TWO));
        when(otherAppender.apply(methodVisitor, implementationContext, methodDescription))
                .thenReturn(new ByteCodeAppender.Size(ONE * MULTIPLIER, TWO * MULTIPLIER));
    }

    @Test
    public void testSkippedMethod() throws Exception {
        assertThat(TypeWriter.MethodPool.Record.ForInheritedMethod.INSTANCE.getSort(), is(TypeWriter.MethodPool.Record.Sort.SKIPPED));
        TypeWriter.MethodPool.Record.ForInheritedMethod.INSTANCE.apply(classVisitor, implementationContext);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(implementationContext);
        verifyZeroInteractions(methodAttributeAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testSkippedMethodCannotBePrepended() throws Exception {
        TypeWriter.MethodPool.Record.ForInheritedMethod.INSTANCE.prepend(byteCodeAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testSkippedMethodCannotBeAppliedBody() throws Exception {
        TypeWriter.MethodPool.Record.ForInheritedMethod.INSTANCE.applyBody(methodVisitor, implementationContext);
    }

    @Test(expected = IllegalStateException.class)
    public void testSkippedMethodCannotBeAppliedHead() throws Exception {
        TypeWriter.MethodPool.Record.ForInheritedMethod.INSTANCE.applyHead(methodVisitor);
    }

    @Test
    public void testDefinedMethod() throws Exception {
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithoutBody(methodDescription,
                methodAttributeAppender,
                modifierResolver);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.DEFINED));
        record.apply(classVisitor, implementationContext);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefinedMethodHeadOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithoutBody(methodDescription,
                methodAttributeAppender,
                modifierResolver);
        record.applyHead(methodVisitor);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verifyZeroInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefinedMethodBodyOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithoutBody(methodDescription,
                methodAttributeAppender,
                modifierResolver);
        record.applyBody(methodVisitor, implementationContext);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
    }

    @Test
    public void testDefinedMethodWithParameters() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithoutBody(methodDescription,
                methodAttributeAppender,
                modifierResolver);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.DEFINED));
        record.apply(classVisitor, implementationContext);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitParameter(FOO, MODIFIERS);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testDefinedMethodPrepended() throws Exception {
        new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithoutBody(methodDescription, methodAttributeAppender, modifierResolver).prepend(otherAppender);
    }

    @Test
    public void testDefaultValueMethod() throws Exception {
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.STRING);
        when(methodDescription.isDefaultValue(FOO)).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithAnnotationDefaultValue(methodDescription,
                FOO,
                methodAttributeAppender,
                modifierResolver);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.DEFINED));
        record.apply(classVisitor, implementationContext);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitAnnotationDefault();
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verify(annotationVisitor).visit(null, FOO);
        verify(annotationVisitor).visitEnd();
        verifyNoMoreInteractions(annotationVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefaultValueMethodHeadOnly() throws Exception {
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.STRING);
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        when(methodDescription.isDefaultValue(FOO)).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithAnnotationDefaultValue(methodDescription,
                FOO,
                methodAttributeAppender,
                modifierResolver);
        record.applyHead(methodVisitor);
        verify(methodVisitor).visitAnnotationDefault();
        verifyNoMoreInteractions(methodVisitor);
        verify(annotationVisitor).visit(null, FOO);
        verify(annotationVisitor).visitEnd();
        verifyNoMoreInteractions(annotationVisitor);
        verifyZeroInteractions(implementationContext);
        verifyZeroInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefaultValueMethodBodyOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithAnnotationDefaultValue(methodDescription,
                FOO,
                methodAttributeAppender,
                modifierResolver);
        record.applyBody(methodVisitor, implementationContext);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefaultValueMethodWithParameters() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.STRING);
        when(methodDescription.isDefaultValue(FOO)).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithAnnotationDefaultValue(methodDescription,
                FOO,
                methodAttributeAppender,
                modifierResolver);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.DEFINED));
        record.apply(classVisitor, implementationContext);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitParameter(FOO, MODIFIERS);
        verify(methodVisitor).visitAnnotationDefault();
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verify(annotationVisitor).visit(null, FOO);
        verify(annotationVisitor).visitEnd();
        verifyNoMoreInteractions(annotationVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultValueMethodPrepended() throws Exception {
        new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithAnnotationDefaultValue(methodDescription,
                FOO,
                methodAttributeAppender,
                modifierResolver).prepend(otherAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoDefaultValue() throws Exception {
        when(methodDescription.isDefaultValue(FOO)).thenReturn(false);
        new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithAnnotationDefaultValue(methodDescription,
                FOO,
                methodAttributeAppender,
                modifierResolver).apply(classVisitor, implementationContext);
    }

    @Test
    public void testImplementedMethod() throws Exception {
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithBody(methodDescription,
                byteCodeAppender,
                methodAttributeAppender,
                modifierResolver);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.IMPLEMENTED));
        record.apply(classVisitor, implementationContext);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitMaxs(ONE, TWO);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
        verify(byteCodeAppender).apply(methodVisitor, implementationContext, methodDescription);
        verifyNoMoreInteractions(byteCodeAppender);
    }

    @Test
    public void testImplementedMethodHeadOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithBody(methodDescription,
                byteCodeAppender,
                methodAttributeAppender,
                modifierResolver);
        record.applyHead(methodVisitor);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verifyZeroInteractions(methodAttributeAppender);
        verifyZeroInteractions(byteCodeAppender);
    }

    @Test
    public void testImplementedMethodBodyOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithBody(methodDescription,
                byteCodeAppender,
                methodAttributeAppender,
                modifierResolver);
        record.applyBody(methodVisitor, implementationContext);
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitMaxs(ONE, TWO);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
        verify(byteCodeAppender).apply(methodVisitor, implementationContext, methodDescription);
        verifyNoMoreInteractions(byteCodeAppender);
    }

    @Test
    public void testImplementedMethodWithParameters() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithBody(methodDescription,
                byteCodeAppender,
                methodAttributeAppender,
                modifierResolver);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.IMPLEMENTED));
        record.apply(classVisitor, implementationContext);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitParameter(FOO, MODIFIERS);
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitMaxs(ONE, TWO);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
        verify(byteCodeAppender).apply(methodVisitor, implementationContext, methodDescription);
        verifyNoMoreInteractions(byteCodeAppender);
    }

    @Test
    public void testImplementedMethodPrepended() throws Exception {
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDeclaredMethod.WithBody(methodDescription,
                byteCodeAppender,
                methodAttributeAppender,
                modifierResolver)
                .prepend(otherAppender);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.IMPLEMENTED));
        record.apply(classVisitor, implementationContext);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitMaxs(ONE * MULTIPLIER, TWO * MULTIPLIER);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
        verify(byteCodeAppender).apply(methodVisitor, implementationContext, methodDescription);
        verifyNoMoreInteractions(byteCodeAppender);
        verify(otherAppender).apply(methodVisitor, implementationContext, methodDescription);
        verifyNoMoreInteractions(otherAppender);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeWriter.MethodPool.Record.ForDeclaredMethod.WithBody.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.MethodPool.Record.ForDeclaredMethod.WithoutBody.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.MethodPool.Record.ForDeclaredMethod.WithAnnotationDefaultValue.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.MethodPool.Record.ForInheritedMethod.class).apply();
    }
}