package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameter;
import uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterType;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataProcessingException;

import java.util.*;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.*;

@Component
public class DateTimeValueFormatter extends CaseViewFieldProcessor {

    private static final List<String> SUPPORTED_TYPES = Arrays.asList(DATETIME, DATE);
    private static final Map<DisplayContext, DisplayContextParameterType> ENUM_MAP = new EnumMap<>(DisplayContext.class);

    private final DateTimeFormatParser dateTimeFormatParser;

    static {
        ENUM_MAP.put(DisplayContext.MANDATORY, DisplayContextParameterType.DATETIMEENTRY);
        ENUM_MAP.put(DisplayContext.OPTIONAL, DisplayContextParameterType.DATETIMEENTRY);
        ENUM_MAP.put(DisplayContext.READONLY, DisplayContextParameterType.DATETIMEDISPLAY);
    }

    @Autowired
    public DateTimeValueFormatter(DateTimeFormatParser dateTimeFormatParser,
                                  CaseViewFieldBuilder caseViewFieldBuilder) {
        super(caseViewFieldBuilder);
        this.dateTimeFormatParser = dateTimeFormatParser;
    }

    @Override
    protected CaseViewField executeSimple(CaseViewField caseViewField, BaseType baseType) {
        caseViewField.setFormattedValue(
            caseViewField.getValue() instanceof TextNode ?
                executeSimple((TextNode) caseViewField.getValue(), caseViewField, baseType, caseViewField.getId(), null, caseViewField) :
                caseViewField.getValue()
        );

        return caseViewField;
    }

    @Override
    protected CaseViewField executeCollection(CaseViewField caseViewField) {
        caseViewField.setFormattedValue(
            caseViewField.getValue() instanceof ArrayNode ?
                executeCollection((ArrayNode) caseViewField.getValue(), caseViewField, caseViewField.getId(), null, caseViewField) :
                caseViewField.getValue()
        );

        return caseViewField;
    }

    @Override
    protected JsonNode executeSimple(JsonNode node, CommonField field, BaseType baseType, String fieldPath, WizardPageComplexFieldOverride override, CommonField topLevelField) {
        final DisplayContext displayContext = displayContext(topLevelField, override);
        return !isNullOrEmpty(node)
            && field.hasDisplayContextParameter(ENUM_MAP.get(displayContext))
            && isSupportedBaseType(baseType, SUPPORTED_TYPES) ?
            createNode(field, node.asText(), baseType, fieldPath, displayContext) :
            node;
    }

    @Override
    protected JsonNode executeCollection(JsonNode collectionNode, CommonField field, String fieldPath, WizardPageComplexFieldOverride override, CommonField topLevelField) {
        final BaseType collectionFieldType = BaseType.get(field.getFieldType().getCollectionFieldType().getType());
        final DisplayContext displayContext = displayContext(topLevelField, override);

        if ((field.hasDisplayContextParameter(ENUM_MAP.get(displayContext))
            && isSupportedBaseType(collectionFieldType, SUPPORTED_TYPES))
            || BaseType.get(COMPLEX) == collectionFieldType) {
            ArrayNode newNode = MAPPER.createArrayNode();
            collectionNode.forEach(item -> {
                JsonNode newItem = item.deepCopy();
                ((ObjectNode)newItem).replace(CollectionValidator.VALUE,
                    isSupportedBaseType(collectionFieldType, SUPPORTED_TYPES) ?
                        createNode(field, item.get(CollectionValidator.VALUE).asText(), collectionFieldType, fieldPath, displayContext) :
                        executeComplex(item.get(CollectionValidator.VALUE), field.getFieldType().getChildren(), null, fieldPath, topLevelField));
                newNode.add(newItem);
            });

            return newNode;
        }

        return collectionNode;
    }

    private TextNode createNode(CommonField field, String valueToConvert, BaseType baseType, String fieldPath, DisplayContext displayContext) {
        if (Strings.isNullOrEmpty(valueToConvert)) {
            return new TextNode(valueToConvert);
        }
        if (ENUM_MAP.containsKey(displayContext)) {
            String format = format(field, displayContext, baseType);
            try {
                if (baseType == BaseType.get(DATETIME)) {
                    return new TextNode(dateTimeFormatParser.convertIso8601ToDateTime(format, valueToConvert));
                } else {
                    return new TextNode(dateTimeFormatParser.convertIso8601ToDate(format, valueToConvert));
                }
            } catch (Exception e) {
                throw new DataProcessingException().withDetails(
                    String.format("Unable to process field %s with value %s. Expected format to be either %s or %s",
                        fieldPath,
                        valueToConvert,
                        format,
                        baseType == BaseType.get(DATETIME) ? DateTimeFormatParser.DATE_TIME_FORMAT : DateTimeFormatParser.DATE_FORMAT)
                );
            }
        }

        return new TextNode(valueToConvert);
    }

    private String format(CommonField field, DisplayContext displayContext, BaseType baseType) {
        return field.getDisplayContextParameter(ENUM_MAP.get(displayContext))
            .map(DisplayContextParameter::getValue)
            .orElseGet(() -> baseType == BaseType.get(DATETIME) ?
                DateTimeFormatParser.DATE_TIME_FORMAT :
                DateTimeFormatParser.DATE_FORMAT
            );
    }
}
