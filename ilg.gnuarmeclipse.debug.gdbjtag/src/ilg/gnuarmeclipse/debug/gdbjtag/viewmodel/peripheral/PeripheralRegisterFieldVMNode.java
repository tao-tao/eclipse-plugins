/*******************************************************************************
 * Copyright (c) 2014 Liviu Ionescu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Liviu Ionescu - initial version 
 *******************************************************************************/

package ilg.gnuarmeclipse.debug.gdbjtag.viewmodel.peripheral;

import ilg.gnuarmeclipse.debug.gdbjtag.datamodel.SvdDMNode;
import ilg.gnuarmeclipse.debug.gdbjtag.datamodel.SvdEnumeratedValueDMNode;
import ilg.gnuarmeclipse.debug.gdbjtag.datamodel.SvdFieldDMNode;

import java.math.BigInteger;

import org.eclipse.debug.core.DebugException;

public class PeripheralRegisterFieldVMNode extends PeripheralRegisterVMNode {

	// ------------------------------------------------------------------------

	private SvdEnumeratedValueDMNode fEnumeratedValueDMNode;

	// ------------------------------------------------------------------------

	public PeripheralRegisterFieldVMNode(PeripheralTreeVMNode parent,
			SvdDMNode dmNode) {

		super(parent, dmNode);

		fValue.setDisplayFormat(PeripheralValue.FORMAT_HEX);

		fEnumeratedValueDMNode = null;
	}

	// ------------------------------------------------------------------------

	public SvdEnumeratedValueDMNode getEnumeratedValueDMNode() {
		if (fEnumeratedValueDMNode == null) {
			try {
				fEnumeratedValueDMNode = ((SvdFieldDMNode) fDMNode)
						.findEnumeratedValue((PeripheralValue) getValue());
			} catch (DebugException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return fEnumeratedValueDMNode;
	}

	public void clearEnumeratedValueDMNode() {
		fEnumeratedValueDMNode = null;
	}

	@Override
	public String getDisplayValue() {

		String readAction = getReadAction();
		if (!readAction.isEmpty()) {
			return "(read " + readAction + ")";
		}

		String value = getValueString();

		if (value.isEmpty()) {
			return null;
		}

		if (!isEnumeration()) {
			return value;
		}

		SvdEnumeratedValueDMNode node = getEnumeratedValueDMNode();
		if (node == null) {
			return value;
		}

		String enumerationName = node.getName();
		if (!enumerationName.isEmpty()) {
			value += ": " + enumerationName;
		}
		return value;
	}

	@Override
	public boolean supportsValueModification() {

		// Prevent write only fields to be modified
		return !isReadOnly() && isReadAllowed();
	}

	@Override
	public boolean verifyValue(String expression) throws DebugException {

		// System.out.println("verifyValue(" + expression + ")");

		boolean isValid = false;
		if (false && isEnumeration()) {
			// TODO: implement enumeration
		} else {
			isValid = PeripheralValue.isNumeric(expression);
		}
		return isValid;
	}

	// ------------------------------------------------------------------------

	@Override
	protected void update(BigInteger newValue) {

		boolean sameValue = getPeripheralValue().getBigValue().equals(newValue);

		if (!sameValue) {

			clearEnumeratedValueDMNode();

			// Propagate change to parent.
			PeripheralRegisterVMNode parent = (PeripheralRegisterVMNode) getParent();

			PeripheralValue parentValue = null;
			parentValue = parent.getPeripheralValue();

			BigInteger bigValue;
			if (parentValue.isNumeric()) {
				bigValue = parentValue.getBigValue();
			} else {
				bigValue = BigInteger.ZERO;
			}
			// Mask out existing value, using andNot().
			bigValue = bigValue.andNot(getBitMask().shiftLeft(getOffsetBits()));

			// Mask in new value, using or().
			bigValue = bigValue.or(newValue.and(getBitMask()).shiftLeft(
					getOffsetBits()));

			// Update the parent with the newly computed value.
			// This in turn will use updateFieldValueFromParent() to update
			// all fields, including this one.
			parent.update(bigValue);
		}
	}

	/**
	 * Update the current field after the parent changed.
	 * 
	 * @return true if the field value changed.
	 */
	public void updateFieldValueFromParent() {

		PeripheralRegisterVMNode parent = (PeripheralRegisterVMNode) getParent();
		PeripheralValue parentValue = parent.getPeripheralValue();

		BigInteger bigValue;
		if (parentValue.isNumeric()) {
			bigValue = parentValue.getBigValue();
			bigValue = bigValue.shiftRight(getOffsetBits()).and(getBitMask());
		} else {
			bigValue = BigInteger.ZERO;
		}

		// Update the current field with the newly computed value.
		fHasChanged = getPeripheralValue().update(bigValue);
	}

	@Override
	public String getDisplayNodeType() {
		return "Field";
	}

	@Override
	public String getImageName() {
		return "field";
	}

	@Override
	public String getDisplayAddress() {
		return convertFieldToString();
	}

	@Override
	public String getDisplayOffset() {
		return convertFieldToString();
	}

	private String convertFieldToString() {

		int lsb = getOffsetBits();
		int msb = lsb + getWidthBits() - 1;

		if (lsb == msb) {
			return String.format("[%d]", lsb);
		} else {
			return String.format("[%d:%d]", msb, lsb);
		}
	}

	@Override
	public int getOffsetBits() {
		return ((SvdFieldDMNode) fDMNode).getOffset();
	}

	@Override
	public int getWidthBits() {
		return ((SvdFieldDMNode) fDMNode).getWidthBits();

	}

	public boolean isEnumeration() {
		return ((SvdFieldDMNode) fDMNode).isEnumeration();
	}

	@Override
	public BigInteger getThisBigAddressOffset() {
		return BigInteger.ZERO;
	}

	// ------------------------------------------------------------------------
}