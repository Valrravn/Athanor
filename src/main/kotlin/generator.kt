import java.io.File
import org.json.JSONObject

fun main() {
    // The file path to the JSON
    val filePath = "src/main/schema.json"

    // Read the file content
    val fileContent = File(filePath).readText()

    // Find and display all definitions
    val definitions = extractDefinitions(fileContent)
    println("Definitions Found:")
    definitions.forEach { println(it) }
}

fun extractDefinitions(jsonContent: String): List<Definition> {
    val definitionsList = mutableListOf<Definition>() // List to store Definition objects

    try {
        // Parse the JSON content into a JSONObject
        val root = JSONObject(jsonContent)

        // Extract the top-level definition ("pipeline")
        val topLevelDefinition = createDefinition("pipeline", root)
        definitionsList.add(topLevelDefinition)

        // Check if the top-level "definitions" object exists
        if (root.has("definitions")) {
            val definitionsObject = root.getJSONObject("definitions")

            // Process all top-level definitions
            definitionsObject.keys().forEach { key ->
                val definition = definitionsObject.getJSONObject(key)

                // Create a Definition instance for the current definition
                val definitionObject = createDefinition(key, definition)
                definitionsList.add(definitionObject)

                // Process nested definitions recursively via the "definitions" field
                if (definition.has("definitions")) {
                    val nestedDefinitions = definition.getJSONObject("definitions")
                    processNestedDefinitions(nestedDefinitions, definitionsList)
                }
            }
        }
    } catch (e: Exception) {
        println("An error occurred while parsing the JSON: ${e.message}")
    }

    return definitionsList // Return the list of Definition objects
}

// Recursive function to process "definitions" field
fun processNestedDefinitions(definitionsObject: JSONObject, definitionsList: MutableList<Definition>) {
    definitionsObject.keys().forEach { key ->
        val nestedDefinition = definitionsObject.getJSONObject(key)

        // Create a Definition instance for the nested definition
        val definitionObject = createDefinition(key, nestedDefinition)
        definitionsList.add(definitionObject)

        // If this nested definition has its own "definitions" field, recurse into it
        if (nestedDefinition.has("definitions")) {
            val deeperNestedDefinitions = nestedDefinition.getJSONObject("definitions")
            processNestedDefinitions(deeperNestedDefinitions, definitionsList)
        }
    }
}

// Function to create a Definition object from a JSON definition
fun createDefinition(name: String, json: JSONObject): Definition {
    return Definition(
        name = name,
        title = json.optString("title", ""), // Get "title" or default to an empty string
        description = json.optString("description", ""), // Get "description" or default to an empty string
        properties = if (json.has("properties")) {
            json.getJSONObject("properties").keys().asSequence().toList() // Extract property names
        } else {
            emptyList()
        },
        required = if (json.has("required")) {
            json.getJSONArray("required").let { array ->
                (0 until array.length()).map { index -> array.getString(index) }
            }
        } else {
            emptyList()
        },
        additionalProperties = if (json.has("additionalProperties")) {
            listOf(json.get("additionalProperties").toString()) // Store as a list, even if it's a single value
        } else {
            emptyList()
        },
        definitions = if (json.has("definitions")) {
            json.getJSONObject("definitions").keys().asSequence().toList() // Extract nested definition names
        } else {
            emptyList()
        }
    )
}

// Data class for a definition
data class Definition(
    val name: String,
    val title: String,
    val description: String,
    val properties: List<String>,
    val required: List<String>,
    val additionalProperties: List<String>,
    val definitions: List<String>
)