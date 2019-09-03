package hyperopt;

// import ntuple.NTupleBanditEA;
import agents.evo.EvoAgent;
import caveswing.core.CaveGameState;
import caveswing.core.CaveSwingParams;
import ggi.agents.EvoAgentFactory;
import hyperopt.ThriftFiles.gen_java.thrift_elites.ParamEvaluator;
import hyperopt.ThriftFiles.gen_java.thrift_elites.Results;

import math.Vector2d;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;

// Thrift
// Thrift is open source software for generating code for inter process communication between various languages
// The idea is you write a small file (.thrift file) defining services (function signitures and data structires build from basic data types)
// And thrift will generate all the networking, serialization, translation and glue code for you.
// Windows compiler: https://www.apache.org/dyn/closer.cgi?path=/thrift/0.12.0/thrift-0.12.0.exe
// To generate the files run:
// thrift-0.12.0.exe -r --gen py map_elites.thrift
// thrift-0.12.0.exe -r --gen java map_elites.thrift

// We will have a java server and a python client. The client will pass a set of parameters to the server, which returs the results and statistics.


public class TuneMapElites
{

    public static class ParamEvaluatorHandler implements ParamEvaluator.Iface
    {
        public void ping() throws org.apache.thrift.TException
        {
            System.out.println("PING");
        }

        public Results evaluate_params(java.util.Map<java.lang.String,java.lang.Double> params) throws org.apache.thrift.TException
        {
            /*
            receive params and update the game and ai params
            run game and collect the data
            send it back to server */


            setParams(params);
            Results res = PlayGame(params);

            return res;
        }
        public Results human_play(java.util.Map<java.lang.String,java.lang.Double> params) throws org.apache.thrift.TException
        {
            /*
            receive params and update the game and ai params
            run game and collect the data
            send it back to server */
            System.out.println("human gameplay");


            setParams(params);
            Results res = PlayGame(params);

            return res;
        }
        public Results visual_evaluation(java.util.Map<java.lang.String,java.lang.Double> params) throws org.apache.thrift.TException
        {
            /*
            receive params and update the game and ai params
            run game and collect the data
            send it back to server */
            System.out.println("RHEA visual gameplay");

            setParams(params);
            Results res = PlayGame(params);

            return res;
        }

    }

    public static Results PlayGame(java.util.Map<java.lang.String,java.lang.Double> params){

        // use fixed agent
        int nEvals = 20;
        int seqLength = 100;
        boolean useShiftBuffer = true;

        // initialize stats
        double height = 0;
        double averageSpeed;
        double score ;
        int ticks;
        int ropeActions = 0;

        // get agent and game from the given parameters
        EvoAgent player = getEvoAgentFromFactory(nEvals, seqLength, useShiftBuffer);
        CaveSwingParams caveParams = setParams(params);
        CaveGameState gameState = new CaveGameState().setParams(caveParams).setup();

        // run main game loop
        while (!gameState.isTerminal()) {
            // get the action from the player, update the game state, and visualize it
            int action = player.getAction(gameState.copy(), 0);
            int[] actions = new int[]{action};
            if (actions[0] == 1)
                ropeActions++;
            gameState.next(actions);
            height += gameState.avatar.s.y;
        }

        // collect stats
        // TODO you can add new behaviour descriptors here
        score = gameState.getScore();
        ticks = gameState.nTicks;
        height /= ticks;
        averageSpeed = caveParams.width/ticks;
        System.out.println((int) gameState.getScore());

        // add result into the correct format
        Results res = new Results();
        res.game_score=score;
        java.util.Map<java.lang.String,java.lang.Double> behaviourMap = new java.util.HashMap<String,Double>();
        behaviourMap.put("height",height);
        behaviourMap.put("averageSpeed",averageSpeed);
        behaviourMap.put("ticks",(double)ticks);
        behaviourMap.put("ropeActions",(double)ropeActions);
        res.behaviour = behaviourMap;

        return res;
    }
    public static EvoAgent getEvoAgentFromFactory(int nEvals, int seqLength, boolean useShiftBuffer) {

        // create agent with the correct parameters
        EvoAgentFactory factory = new EvoAgentFactory();
        factory.nEvals = nEvals;
        factory.seqLength = seqLength;
        factory.useShiftBuffer = useShiftBuffer;
        EvoAgent evoAgent = factory.getAgent();
        evoAgent.setUseShiftBuffer(true);

        return evoAgent;
    }

    public static CaveSwingParams setParams(java.util.Map<java.lang.String,java.lang.Double> params) {
        // process parameters
        CaveSwingParams newParams = new CaveSwingParams();
        newParams.pointPerX = params.get("pointPerX").intValue();
        newParams.pointPerY = params.get("pointPerY").intValue();
        newParams.hooke = params.get("hooke");
        newParams.gravity = new Vector2d( params.get("gravity_X"),params.get("gravity_Y"));
        newParams.width = params.get("width").intValue();
        newParams.nAnchors = params.get("nAnchors").intValue();
        newParams.maxTicks = params.get("maxTicks").intValue();
        newParams.meanAnchorHeight = params.get("meanAnchorHeight");
        newParams.costPerTick = params.get("costPerTick").intValue();
        newParams.lossFactor = params.get("lossFactor");
        newParams.failurePenalty = params.get("failurePenalty").intValue();
        newParams.successBonus = params.get("successBonus").intValue();
        newParams.pointPerX = params.get("pointPerX").intValue();
        newParams.pointPerX = params.get("pointPerX").intValue();

        return newParams;

    }

    public static void simple(ParamEvaluator.Processor processor) {
        try {
            TServerTransport serverTransport = new TServerSocket(9090);
            TServer server = new TSimpleServer(new Args(serverTransport).processor(processor));

            // Use this for a multithreaded server
            // TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));

            System.out.println("Starting the simple server...");
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        // code for starting up a client
        try {
            ParamEvaluatorHandler handler = new ParamEvaluatorHandler();
            ParamEvaluator.Processor processor = new ParamEvaluator.Processor(handler);

            Runnable simple = () -> simple(processor);

            new Thread(simple).start();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

}