package networkservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import java.io.FileNotFoundException

/**
 * Utility object providing helper methods for loading JSON schemas
 * used in network message validation tests.
 */
object Util {

    /** Object mapper used to parse JSON schema files. */
    private val mapper = jacksonObjectMapper()

    /**
     * Loads a JSON schema from the test resources and returns it as a
     * {@link JsonSchema} instance.
     *
     * The schema is loaded from the given resource path, parsed into a JSON
     * tree, and converted into a Draft-7 JSON schema.
     *
     * @param filepath the relative path of the schema file within the test
     * resources
     * @return the loaded {@link JsonSchema}
     * @throws FileNotFoundException if the schema file cannot be found
     */
    fun loadSchema(filepath: String): JsonSchema {

        val inputStream = Util::class.java.classLoader.getResourceAsStream(filepath)
        if (inputStream == null) {
            throw FileNotFoundException("Schema file not found in test resources: $filepath")
        }

        val schemaNode = mapper.readTree(inputStream)

        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(schemaNode)
    }
}