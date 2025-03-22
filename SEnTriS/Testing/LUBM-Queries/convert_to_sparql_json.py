import json
import os

def convert_to_sparql_json(input_file, output_file):
    with open(input_file, 'r') as file:
        lines = file.readlines()
    
    # Ensure fresh data for each file
    sparql_json = {"head": {}, "results": {}}
    
    # Handle case where file contains "NO ANSWERS."
    if len(lines) == 1 and lines[0].strip() == "NO ANSWERS.":
        sparql_json = {
            "head": {"vars": []},
            "results": {"bindings": []}
        }
    else:
        # Extract variables from the first line
        variables = lines[0].strip().split('\t')
        
        # Extract bindings
        bindings = []
        for line in lines[1:]:
            values = line.strip().split('\t')
            if len(values) == len(variables):
                binding = {variables[i]: {"type": determine_type(values[i]), "value": values[i]} for i in range(len(variables))}
                bindings.append(binding)
        
        # Create SPARQL JSON structure
        sparql_json = {
            "head": {"vars": variables},
            "results": {"bindings": bindings}
        }
    
    # Save to output file
    with open(output_file, 'w') as out_file:
        json.dump(sparql_json, out_file, indent=2)
    
    print(f"SPARQL JSON saved to {output_file}")

def determine_type(value):
    return "uri" if value.startswith("http://") or value.startswith("https://") else "literal"

def format_answers(directory):
    output_dir = directory + "/json_answers"
    os.makedirs(output_dir, exist_ok=True)
    input_dir = directory + "/answers"
    for filename in os.listdir(input_dir):
        input_path = input_dir + "/" + filename
        if os.path.isfile(input_path) and filename.endswith(".txt"):
            output_filename = filename.replace(".txt", ".json")
            output_path = output_dir + "/" + output_filename
            convert_to_sparql_json(input_path, output_path)

if __name__ == "__main__":
    directory = os.getcwd()  # Change this to the desired directory
    format_answers(directory)
