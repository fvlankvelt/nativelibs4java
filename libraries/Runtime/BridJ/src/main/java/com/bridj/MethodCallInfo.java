/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;
import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import static com.bridj.Dyncall.*;
import static com.bridj.Dyncall.CallingConvention.*;
import com.bridj.ann.CallingConvention;
import static com.bridj.Dyncall.SignatureChars.*;
import com.bridj.*;
import com.bridj.ann.*;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
/**
 *
 * @author Olivier
 */
public class MethodCallInfo {

    /*public static class GenericMethodInfo {
        Type returnType, paramsTypes[];
    }
    GenericMethodInfo genericInfo = new GenericMethodInfo();*/
    int returnValueType, paramsValueTypes[];
	Method method;
	long forwardedPointer;
    String dcSignature;
	String javaSignature;
	Callback javaCallback;
	int virtualIndex = -1;
	int virtualTableOffset = 0;
    int dcCallingConvention = DC_CALL_C_DEFAULT;

	boolean isVarArgs;
	boolean isStatic;
	boolean isCPlusPlus;
	
	boolean direct;
	boolean startsWithThis;
    boolean isCallableAsRaw;

	public MethodCallInfo(Method method, NativeLibrary library) throws FileNotFoundException {
        isVarArgs = false;
        isCPlusPlus = false;
        this.method = method;
        forwardedPointer = BridJ.getSymbolAddress(library, method);

        Class<?>[] paramsTypes = method.getParameterTypes();
        Annotation[][] paramsAnnotations = method.getParameterAnnotations();
        /*genericInfo.returnType = method.getGenericReturnType();
        genericInfo.paramsTypes = method.getGenericParameterTypes();*/
        
        int modifiers = method.getModifiers();
        isStatic = Modifier.isStatic(modifiers);
        isVarArgs = method.isVarArgs();

        int nParams = paramsTypes.length;
        paramsValueTypes = new int[nParams];

        this.direct = nParams <= JNI.getMaxDirectMappingArgCount();
        
        isCallableAsRaw = true; // TODO on native side : test number of parameters (on 64 bits win : must be <= 4)
        //isCPlusPlus = CPPObject.class.isAssignableFrom(method.getDeclaringClass());

        //GetOptions(methodOptions, method);

        StringBuilder javaSig = new StringBuilder(64), dcSig = new StringBuilder(16);
        javaSig.append('(');
        dcSig.append(DC_SIGCHAR_POINTER).append(DC_SIGCHAR_POINTER); // JNIEnv*, jobject: always present in native-bound functions

        for (int iParam = 0; iParam < nParams; iParam++) {
//            Options paramOptions = paramsOptions[iParam] = new Options();
            Class<?> param = paramsTypes[iParam];

            ValueType paramValueType = getValueType(iParam, param, null, paramsAnnotations[iParam]);
            paramsValueTypes[iParam] = paramValueType.ordinal();
            //GetOptions(paramOptions, method, paramsAnnotations[iParam]);

            appendToSignature(paramValueType, javaSig, dcSig);
        }
        javaSig.append(')');
        dcSig.append(')');

        ValueType retType = getValueType(-1, method.getReturnType(), method);
        appendToSignature(retType, javaSig, dcSig);
        returnValueType = retType.ordinal();

        javaSignature = javaSig.toString();
        dcSignature = dcSig.toString();
        
        
        Virtual virtual = BridJ.getAnnotation(Virtual.class, false, method);
        isCPlusPlus = isCPlusPlus || virtual != null;
        
        CallingConvention cc = BridJ.getAnnotation(CallingConvention.class, false, method);
    	if (isCPlusPlus) {
        	if (JNI.isWindows()) {
        		if (!JNI.is64Bits())
        			dcCallingConvention = DC_CALL_C_X86_WIN32_THIS_MS;
        	} else {
        		if (!JNI.is64Bits())
        			dcCallingConvention = DC_CALL_C_X86_WIN32_THIS_GNU;
        	}
        } else {
        	if (cc != null) {
        		switch (cc.value()) {
        		case Auto:
        			break;
        		case FastCall:
        			dcCallingConvention = JNI.isWindows() ? DC_CALL_C_X86_WIN32_FAST_MS : DC_CALL_C_X86_WIN32_FAST_GNU;
        			break;
        		case StdCall:
        			dcCallingConvention = DC_CALL_C_X86_WIN32_STD;
        			break;
        		}
        	}
        }
    }
	

	public String getDcSignature() {
		return dcSignature;
	}
	public String getJavaSignature() {
		return javaSignature;
	}
    boolean getBoolAnnotation(Class<? extends Annotation> ac, boolean inherit, AnnotatedElement element, Annotation... directAnnotations) {
        Annotation ann = BridJ.getAnnotation(ac, inherit, element, directAnnotations);
        return ann != null;
    }
    public ValueType getValueType(int iParam, Class<?> c, AnnotatedElement element, Annotation... directAnnotations) {
    	PointerSized sz = BridJ.getAnnotation(PointerSized.class, true, element, directAnnotations);
    	This th = BridJ.getAnnotation(This.class, true, element, directAnnotations);
    	CLong cl = BridJ.getAnnotation(CLong.class, true, element, directAnnotations);
        
    	if (sz != null || th != null || cl != null) {
    		if (!(c == Long.class || c == Long.TYPE))
    			throw new RuntimeException("Annotation should only be used on a long parameter, not on a " + c.getName());
    		
    		if (sz != null) {
                isCallableAsRaw = isCallableAsRaw && JNI.is64Bits();
            } else if (cl != null) {
                isCallableAsRaw = isCallableAsRaw && (JNI.CLONG_SIZE == 8);
            } else if (th != null) {
            	isCPlusPlus = true;
				startsWithThis = true;
				if (iParam != 0)
					throw new RuntimeException("Annotation " + This.class.getName() + " can only be used on the first parameter");
            }
    	    return ValueType.eSizeTValue;
    	}
    	if (c == null || c.equals(Void.TYPE))
            return ValueType.eVoidValue;
        if (c == Integer.class || c == Integer.TYPE)
            return ValueType.eIntValue;
        if (c == Long.class || c == Long.TYPE) {
        	return ValueType.eLongValue;
        }
        if (c == Short.class || c == Short.TYPE)
            return ValueType.eShortValue;
        if (c == Byte.class || c == Byte.TYPE)
            return ValueType.eByteValue;
        if (c == Float.class || c == Float.TYPE)
            return ValueType.eFloatValue;
        if (c == Double.class || c == Double.TYPE)
            return ValueType.eDoubleValue;
        if (c == Boolean.class || c == Boolean.TYPE)
            return ValueType.eByteValue;

        throw new NoSuchElementException("No " + ValueType.class.getSimpleName() + " for class " + c.getName());
    }

    public void appendToSignature(ValueType type, StringBuilder javaSig, StringBuilder dcSig) {
        char dcChar, javaChar;
        switch (type) {
            case eVoidValue:
                dcChar = DC_SIGCHAR_VOID;
                javaChar = 'V';
                break;
            case eIntValue:
                dcChar = DC_SIGCHAR_INT;
                javaChar = 'I';
                break;
            case eLongValue:
                dcChar = DC_SIGCHAR_LONGLONG;
                javaChar = 'J';
                break;
            case eSizeTValue:
                if (JNI.SIZE_T_SIZE == 8) {
                    dcChar = DC_SIGCHAR_LONGLONG;
                    javaChar = 'J';
                } else {
                    dcChar = DC_SIGCHAR_INT;
                    javaChar = 'I';
                    isCallableAsRaw = false;
                }
                break;
            case eShortValue:
                dcChar = DC_SIGCHAR_SHORT;
                javaChar = 'S';
                break;
            case eDoubleValue:
                dcChar = DC_SIGCHAR_DOUBLE;
                javaChar = 'D';
                break;
            case eFloatValue:
                dcChar = DC_SIGCHAR_FLOAT;
                javaChar = 'F';
                break;
            case eByteValue:
                dcChar = DC_SIGCHAR_CHAR;
                javaChar = 'B';
                break;
            case eWCharValue:
                switch (JNI.WCHAR_T_SIZE) {
                case 1:
                    dcChar = DC_SIGCHAR_CHAR;
                    isCallableAsRaw = false;
                    break;
                case 2:
                    dcChar = DC_SIGCHAR_SHORT;
                    break;
                case 4:
                    dcChar = DC_SIGCHAR_INT;
                    isCallableAsRaw = false;
                    break;
                default:
                    throw new RuntimeException("Unhandled sizeof(wchar_t) in GetJavaTypeSignature: " + JNI.WCHAR_T_SIZE);
                }
                javaChar = 'C';
                break;
            default:
                throw new RuntimeException("Unhandled " + ValueType.class.getSimpleName() + ": " + type);
        }
        if (javaSig != null)
            javaSig.append(javaChar);
        if (dcSig != null)
            dcSig.append(dcChar);
    }

}