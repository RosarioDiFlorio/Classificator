import pickle
import sys
import json

pathfile = sys.argv[1]
words = pickle.load( open(pathfile, "rb" ) )

def dumper(obj):
    try:
        return obj.toJSON()
    except:
        return str(obj)

print(json.dumps(words,default=dumper))

exit()