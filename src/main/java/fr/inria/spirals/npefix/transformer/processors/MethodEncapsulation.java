package fr.inria.spirals.npefix.transformer.processors;

import fr.inria.spirals.npefix.resi.CallChecker;
import fr.inria.spirals.npefix.resi.exception.ForceReturn;
import fr.inria.spirals.npefix.transformer.utils.IConstants;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.*;
import spoon.support.reflect.code.*;
import spoon.support.reflect.reference.CtFieldReferenceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ajoute les try catch autour des methodes
 * @author bcornu
 *
 */
@SuppressWarnings("all")
public class MethodEncapsulation extends AbstractProcessor<CtMethod> {

	private static int methodNumber = 0;
	
	public static int getCpt(){
		return methodNumber;
	}


	@Override
	public boolean isToBeProcessed(CtMethod ctMethode) {
		methodNumber++;
		if(ctMethode.getBody() == null)
			return false;
		if(ctMethode.getType() instanceof CtTypeParameterReference) {
			return false;
		}
		return true;
	}

	@Override
	public void process(CtMethod ctMethode) {
		CtLocalVariable methodVar = getNewMethodcontext();
		
		CtTypeReference tmpref = getFactory().Core().clone(ctMethode.getType());
		if(!(tmpref instanceof CtArrayTypeReference)){
			tmpref = tmpref.box();
		}else if(((CtArrayTypeReference)tmpref).getComponentType()!=null){
			((CtArrayTypeReference)tmpref).getComponentType().setActualTypeArguments(null);
		}
//		tmpref.setActualTypeArguments(null);
		
		CtTry coreTry = createTry(methodVar,tmpref);
		if(coreTry == null) {
			return;
		}
		coreTry.setBody(getFactory().Core().createBlock());
		coreTry.getBody().setStatements(ctMethode.getBody().getStatements());
		
		List<CtStatement> stats = new ArrayList<CtStatement>();
		stats.add(methodVar);
		stats.add(coreTry);
		
		ctMethode.getBody().setStatements(stats);
	}
	
	private CtTry createTry(CtLocalVariable methodVar, CtTypeReference tmpref){
		CtVariableAccessImpl methodAccess = new CtVariableReadImpl();
		methodAccess.setVariable(methodVar.getReference());
		
		CtReturn ret = getFactory().Core().createReturn();

		if(!tmpref.equals(getFactory().Code().createCtTypeReference(Void.class))){
			CtTypeReference tmp2 = tmpref;
			CtExpression arg = null;
			if(tmp2 instanceof CtArrayTypeReference){
				tmp2=((CtArrayTypeReference)tmp2).getDeclaringType();
			}
			if(tmp2==null || tmp2.isAnonymous() || tmp2.getSimpleName()==null || (tmp2.getPackage()==null && tmp2.getSimpleName().length()==1)){
				arg = getFactory().Core().createLiteral();
				arg.setType(getFactory().Type().nullType());
			}else{
				tmp2 = getFactory().Type().createReference(tmp2.getQualifiedName());
				CtFieldReference ctfe = new CtFieldReferenceImpl();
				ctfe.setSimpleName("class");
				ctfe.setDeclaringType(tmp2);
				
				arg = new CtFieldReadImpl();
				((CtFieldAccessImpl) arg).setVariable(ctfe);
			}

			if((tmp2 instanceof CtTypeParameterReference)) {
				return null;
			}

			CtExecutableReference execref = getFactory().Core().createExecutableReference();
			execref.setDeclaringType(getFactory().Type().createReference(CallChecker.class));
			execref.setSimpleName("returned");
			execref.setStatic(true);
			
			CtInvocationImpl invoc = (CtInvocationImpl) getFactory().Core().createInvocation();
			invoc.setExecutable(execref);
			invoc.setArguments(Arrays.asList(new CtExpression[]{arg}));
			
			ret.setReturnedExpression(invoc);
		}

		CtCatchVariable parameter = getFactory().Core().createCatchVariable();
		parameter.setType(getFactory().Type().createReference(ForceReturn.class));
		parameter.setSimpleName("_bcornu_return_t");
		
		CtCatch localCatch = getFactory().Core().createCatch();
		localCatch.setParameter(parameter);
		localCatch.setBody(getFactory().Core().createBlock());
		localCatch.getBody().addStatement(ret);
		
		CtExecutableReference executableRef = getFactory().Core().createExecutableReference();
		executableRef.setSimpleName("methodEnd");

		CtInvocation invoc = getFactory().Core().createInvocation();
		invoc.setExecutable(executableRef);
		invoc.setTarget(methodAccess);
		
		CtBlock finalizer = getFactory().Core().createBlock();
		finalizer.addStatement(invoc);
		
		CtTry e = getFactory().Core().createTry();
		e.addCatcher(localCatch);
		e.setFinalizer(finalizer);
		return e;
	}
	
	private CtLocalVariable getNewMethodcontext() {
		CtConstructorCall ctx = getFactory().Core().createConstructorCall();
		ctx.setType(getFactory().Type().createReference(IConstants.Class.METHODE_CONTEXT));

		List<CtLiteral> args = new ArrayList<>();

		CtLiteral tryNum = getFactory().Core().createLiteral();
		tryNum.setValue(methodNumber);
		args.add(tryNum);

		CtLocalVariable context = getFactory().Core().createLocalVariable();
		context.setSimpleName(IConstants.Var.METHODE_CONTEXT+ methodNumber);
		context.setType(getFactory().Type().createReference(IConstants.Class.METHODE_CONTEXT));
		context.setDefaultExpression(ctx);
		return context;
	}
}
