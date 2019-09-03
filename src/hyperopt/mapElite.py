import sys, random, copy, string
from tqdm import tqdm
sys.path.append('ThriftFiles/gen-py')

from map_elites import ParamEvaluator
from map_elites.ttypes import Results

from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol

import matplotlib.pyplot as plt
import numpy as np
import matplotlib
import mapElite
import cv2


# TODO can we automatize this?
# this is the discretization of the behaviour variables
# if it is smaller than first index will be 0
# if is between first and second value index is 1 ...
behaviour_tresholds = [("height",
    (37.18849732,  40.68569323,  44.18288914,  47.68008505,
        51.17728096,  54.67447687,  58.17167278,  61.66886869,
        65.1660646 ,  68.66326051,  72.16045642,  75.65765234,
        79.15484825,  82.65204416,  86.14924007,  89.64643598,
        93.14363189,  96.6408278 , 100.13802371, 103.63521962,
       107.13241553, 110.62961145, 114.12680736, 117.62400327,
       121.12119918, 124.61839509, 128.115591  , 131.61278691,
       135.10998282, 138.60717873, 142.10437464, 145.60157056,
       149.09876647, 152.59596238, 156.09315829, 159.5903542 ,
       163.08755011, 166.58474602, 170.08194193, 173.57913784,
       177.07633375, 180.57352967, 184.07072558, 187.56792149,
       191.0651174 , 194.56231331, 198.05950922, 201.55670513,
       205.05390104, 208.55109695)), # height Y AXIS
    ("averageSpeed", 
    (    5.        ,  11.26530612,  17.53061224,  23.79591837,
        30.06122449,  36.32653061,  42.59183673,  48.85714286,
        55.12244898,  61.3877551 ,  67.65306122,  73.91836735,
        80.18367347,  86.44897959,  92.71428571,  98.97959184,
       105.24489796, 111.51020408, 117.7755102 , 124.04081633,
       130.30612245, 136.57142857, 142.83673469, 149.10204082,
       155.36734694, 161.63265306, 167.89795918, 174.16326531,
       180.42857143, 186.69387755, 192.95918367, 199.2244898 ,
       205.48979592, 211.75510204, 218.02040816, 224.28571429,
       230.55102041, 236.81632653, 243.08163265, 249.34693878,
       255.6122449 , 261.87755102, 268.14285714, 274.40816327,
       280.67346939, 286.93877551, 293.20408163, 299.46938776,
       305.73469388, 312.        )) # speed X AXIS
                      ]

# default values for parameters
params = dict()
params["pointPerX"] = 10
params["hooke"] = 0.02
params["width"] = 2500
params["nAnchors"] = 8
params["maxTicks"] = 500
params["meanAnchorHeight"] = 100.0
params["costPerTick"] = 50
params["lossFactor"] = 0.9999
params["failurePenalty"] = 1000
params["pointPerY"] = -10
params["successBonus"] = 1000
params["gravity_X"] = -0.0
params["gravity_Y"] = 1.2


# ranges for parameters
param_limits = dict()
param_limits["pointPerX"] = ("int",10,10)
param_limits["hooke"] = ("float",0.005,0.1)
param_limits["width"] = ("int",2500,2500)
param_limits["nAnchors"] = ("int",5,20)
param_limits["maxTicks"] = ("int",500,500)
param_limits["meanAnchorHeight"] = ("float",100,100)
param_limits["costPerTick"] = ("int",50,50)
param_limits["lossFactor"] = ("float",0.99,0.99999)
param_limits["failurePenalty"] = ("int",1000,1000)
param_limits["pointPerY"] = ("int",-10,-10)
param_limits["successBonus"] = ("int",1000,1000)
param_limits["gravity_X"] = ("float",-1,1)
param_limits["gravity_Y"] = ("float",-1,2)



def getRandomParams(param_limits):
    #new random params dictionary
    randomParams = dict()
    #iterate the dictionary
    for param_name in param_limits:
        #get that param datatype
        if param_limits[param_name][0] == "int":
            randomParams[param_name] = np.random.randint(param_limits[param_name][1],param_limits[param_name][2]+1) # +1 randint upper limit not inclusive
        elif param_limits[param_name][0] == "float":
            randomParams[param_name] = np.random.uniform(param_limits[param_name][1],param_limits[param_name][2])
    return randomParams

def connectToServer():

    # Make socket
    transport = TSocket.TSocket('localhost', 9090)

    # Buffering is critical. Raw sockets are very slow
    transport = TTransport.TBufferedTransport(transport)

    # Wrap in a protocol
    protocol = TBinaryProtocol.TBinaryProtocol(transport)

    # Create a client to use the protocol encoder
    client = ParamEvaluator.Client(protocol)

    # Connect!
    transport.open()
    return client

def init_elite_MAP(behaviour_tresholds):
    dims = [len(bt[1])+1 for bt in behaviour_tresholds]
    return np.empty(dims,dtype=object)


def behavior_to_behaviour_idx(b,b_tresholds):
    b_idx = []
    for name,tresholds in b_tresholds:
        for i,treshold_val in enumerate(tresholds):
            if b[name] < treshold_val:
                b_idx.append(i)
                break
        else: # if for finished without a break
            b_idx.append(len(tresholds))
    return b_idx

def getRandomParams(param_limits):
    #new random params dictionary
    randomParams = dict()
    #iterate the dictionary
    for param_name in param_limits:
        #get that param datatype
        if param_limits[param_name][0] == "int":
            randomParams[param_name] = np.random.randint(param_limits[param_name][1],param_limits[param_name][2]+1) # +1 randint upper limit not inclusive
        elif param_limits[param_name][0] == "float":
            randomParams[param_name] = np.random.uniform(param_limits[param_name][1],param_limits[param_name][2])
    return randomParams

def mutateParams(params,param_limits):

    num_of_params_to_mutate = np.random.randint(1,4)
    params_to_mutate = np.random.choice(list(params.keys()),num_of_params_to_mutate,replace=False)

    for param_name in params_to_mutate:
        current_param_limits = param_limits[param_name]
        if current_param_limits[0] == "int":
            params[param_name] = np.random.randint(current_param_limits[1],current_param_limits[2]+1) # +1 randint upper limit not inclusive
        elif current_param_limits[0] == "float":
            params[param_name] = np.random.uniform(current_param_limits[1],current_param_limits[2])
        else:
            print("the param type is WRONG, not int or float")

    return params

def runSimulation(numberOfIters, GRandomSolutions,client):
    # Create an empty, N-dimensional map of elites
    elites = init_elite_MAP(behaviour_tresholds)  # list of tuples [[score, params]]

    gifIndex = 0
    #loop for numberOfIters iterations
    for x in tqdm(range(numberOfIters)):
        if gifIndex %10 ==0:
            makeHeatMap(elites,x)
        gifIndex +=1
        
        #Initialize by generating G random solutions
        if (x < GRandomSolutions):
            #get a random set of #PARAMS
            randomParams = getRandomParams(param_limits)
            results = client.evaluate_params(randomParams) #get the score and the behavior
            result = results.game_score
            b = results.behaviour
            #get the index
            index = behavior_to_behaviour_idx(b, behaviour_tresholds)
            
            #print(index)
            #evaluate the index
            if(elites[tuple(index)] == None):
                elites[tuple(index)] = (result, randomParams)
            elif((elites[tuple(index)][0]) < result):
                elites[tuple(index)] = (result, randomParams)
        else:
            # Select a random X elite
            randomElite = random.choice(elites.flatten())
            while (randomElite == None):
                randomElite = random.choice(elites.flatten())
            # Modify X via mutation or crossover
            eliteMutated = copy.deepcopy(randomElite)
            #mutate the params
            paramsMutated = mutateParams(eliteMutated[1], param_limits)

            #calculate the performance of the mutated version
            scoresMutated = client.evaluate_params(paramsMutated)
            scoreMutated = scoresMutated.game_score
            b = scoresMutated.behaviour
            eliteMutatedResult = (scoreMutated,paramsMutated)

            #get the index
            behav = behavior_to_behaviour_idx(b, behaviour_tresholds)
            #if it does not exits, populate it
            if(elites[tuple(behav)] == None):
                elites[tuple(behav)] = (scoreMutated, paramsMutated)
            #if the old version is worse, override it
            if (elites[tuple(behav)][0] < behav[0]):
                elites[tuple(behav)] = (scoreMutated, paramsMutated)

        
    return elites


def randomString(stringLength=10):
    """Generate a random string of fixed length """
    letters = string.ascii_lowercase
    return ''.join(random.choice(letters) in range(stringLength))
                   

def makeHeatMap(scores,x):
    scores = np.ones(elites.shape)
    scores = np.negative(scores)

    for i in range(elites.shape[0]):
        for j in range(elites.shape[1]):
            if elites[i,j] != None:
                scores[i,j] = elites[i,j][0]

    scores[scores == -1] = np.nan

    print(scores.shape)

    fig=plt.figure()
    from matplotlib.pyplot import figure
    figure(num=None, figsize=(8, 6), dpi=80)
    current_cmap = matplotlib.cm.get_cmap()
    current_cmap.set_bad(color='white')
    print('making map')
    #plt.axis('off')
    plt.grid()
    plt.imshow(scores, cmap=current_cmap, interpolation='nearest')
    plt.xlabel('Average Speed')
    plt.ylabel('Average Height')

    
    interval = 5
    x1 =5
    x2= 312
    xa = x2-x1
    
    #xa /= interval
    arr = []
    for i in range(0,interval+1):
        arr.append(round(x1 +(xa/interval)*i)) 

    plt.xticks([0, 10, 20, 30, 40,50], arr)

    x1 =208
    x2= 36
    xa = x2-x1
    
    #xa /= interval
    arr = []
    for i in range(0,interval+1):
        arr.append(round(x1 +(xa/interval)*i)) 

    plt.yticks([0, 10, 20, 30, 40,50], arr)
    #plt.colorbar()
    #plt.show()

    # plt.savefig('gifs/foo'+str(x)+'.png', bbox_inches='tight', pad_inches=0)

if __name__ == "__main__":
    numIters = 2000
    randomSolutions = 50

    client = mapElite.connectToServer()
    elites = mapElite.runSimulation(numIters, randomSolutions, client)