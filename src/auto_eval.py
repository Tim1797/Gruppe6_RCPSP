import subprocess
import numpy as np

if __name__ == "__main__":  

    # TODO create different configs also linux
    # ALSO repeat quote:

    repeat = 3

    config_file_path = "default.config"

    import sys

    files = []
    if len(sys.argv) > 1:
        config_file_path = sys.argv[1]

    import os
    if(os.path.isfile(config_file_path)):
        files.append(config_file_path)
    else:
        for (dirpath, dirnames, filenames) in os.walk(config_file_path):
            for f in filenames:
                files.append(config_file_path + f)
            break


    print("Found configs: ", len(files))
    
    f = open("results.txt", "w") 
    
    for config_file in files:
        print("CONFIG:", config_file)

        list_benchmark_files = None
        list_program_calls = None

        import json
        with open(config_file) as json_file:
            data = json.load(json_file)
            list_benchmark_files = data['benchmark_files']
            list_program_calls = data['program_calls']

        data_time = []
        data_count = []
               
        for i, x in enumerate(list_benchmark_files):
            temp_time = []
            temp_count = []
                     
            parts = x.split("/") 
            instanceName = parts[-1].split(".")[0]    
            f.write(instanceName + "\t")     
            
            for j, y in enumerate(list_program_calls):
                makespans = []
                #time = (y.replace("%inputfile%", x)).split()[4]
                #f.write(time + " ")                                
                for o in range(repeat):
                    print(y.replace("%inputfile%", x))
                    
                    output = subprocess.run((y.replace("%inputfile%", x)).replace("%seed%", str(o*7+i*3+j*5)), stdout=subprocess.PIPE, shell=True)
                    
                    outputDecode = output.stdout.decode('utf-8')
                    makespan = (outputDecode.split("\n")[1]).split()[1]
                    makespans.append(int(makespan))
                
                avg_makespan = round(np.average(makespans), 3)  
                
                
                f.write(str(avg_makespan) + "\n")   
                                                                               
        f.close()
        
        
        
        
        
