package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;

import java.text.MessageFormat;

/**
 * A utility class used for checking the applicability of rules.
 *
 * @author Ralf Quast
 */
class Assert {

    private Assert() {
    }

    static void notNull(String value, String name) throws RuleException {
        if (value == null) {
            throw new RuleException(
                    MessageFormat.format("Expected non-null value for property ''{0}''.", name));
        }
    }

    static void notEmpty(String value, String name) throws RuleException {
        if (value.isEmpty()) {
            throw new RuleException(
                    MessageFormat.format("Expected non-empty value for property ''{0}''.", name));
        }
    }

    static void type(String expectedType, VariableDescriptor sourceDescriptor) throws RuleException {
        if (!expectedType.equals(sourceDescriptor.getType())) {
            throw new RuleException(
                    MessageFormat.format("Expected variable type ''{0}'', but actual type is ''{1}''.",
                                         expectedType,
                                         sourceDescriptor.getType()));
        }
    }

    static void unit(String expectedUnit, VariableDescriptor sourceDescriptor) throws RuleException {
        if (!expectedUnit.equals(sourceDescriptor.getUnit())) {
            throw new RuleException(
                    MessageFormat.format("Expected unit ''{0}'', but actual unit is ''{1}''.",
                                         expectedUnit,
                                         sourceDescriptor.getUnit()));
        }
    }
}
