package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;

/**
 * A rule is used for converting {@link VariableDescriptor} properties and for
 * carrying out a corresponding numerical conversion.
 *
 * @author Ralf Quast
 */
public interface Rule {

    /**
     * Applies the rule to the source descriptor supplied as arguments.
     *
     * @param sourceDescriptor The source descriptor.
     *
     * @return the target descriptor resulting from applying this rule to the
     *         source descriptor supplied as argument.
     *
     * @throws RuleException when the rule cannot be applied.
     */
    VariableDescriptor apply(VariableDescriptor sourceDescriptor) throws RuleException;

    /**
     * Applies the numerical conversion rule to the number supplied as argument.
     * <p/>
     * Note that the target descriptor can be obtained by applying this rule to
     * the source descriptor applied as argument.
     *
     * @param number           A number.
     * @param sourceDescriptor A descriptor of the number supplied as argument.
     *
     * @return the converted number.
     *
     * @throws RuleException when the rule cannot be applied.
     */
    Number apply(Number number, VariableDescriptor sourceDescriptor) throws RuleException;
}

