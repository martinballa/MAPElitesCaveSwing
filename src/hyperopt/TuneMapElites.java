package hyperopt;

// import ntuple.NTupleBanditEA;
import agents.evo.EvoAgent;
import caveswing.core.CaveGameState;
import caveswing.core.CaveSwingParams;
import caveswing.util.ViewUtil;
import caveswing.view.CaveView;
import ggi.agents.EvoAgentFactory;
import hyperopt.ThriftFiles.gen_java.thrift_elites.ParamEvaluator;
import hyperopt.ThriftFiles.gen_java.thrift_elites.Results;

import math.Vector2d;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import utilities.ElapsedTimer;
import utilities.JEasyFrame;
import utilities.StatSummary;

import java.util.ArrayList;

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
    static double height=0;
    static double averageSpeed =0;
    static double score =0;
    static double ticks =0;
    static double anchorNormalized = 0;
    static int evalTimes = 20;

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

            System.out.println("Evaluate params");
            setParams(params);
            Results res = PlayGame(params);
            System.out.println("evaluation is done");

            return res;
        }

        public Results run_params(java.util.Map<java.lang.String,java.lang.Double> params) throws org.apache.thrift.TException
        {
            Results res = testParams(params);

            return res;
        }
    }

//    public static Results PlayGame(java.util.Map<java.lang.String,java.lang.Double> params){
//        CaveSwingParams caveParams = setParams(params);
//        EvoAgent player = getEvoAgentFromFactory(params);
//
//        // Reinitialize arrays every time
//        ArrayList<Double> heightList = new ArrayList<>();
//        ArrayList<Double> avgSpeedList = new ArrayList<>();
//        ArrayList<Double> scoreList = new ArrayList<>();
//        ArrayList<Double> ticksList = new ArrayList<>();
//        ArrayList<Double> anchorList = new ArrayList<>();
//        ArrayList<Double> finalPosList = new ArrayList<>();
//        // reset data
//        height =0;
//        averageSpeed =0;
//        score =0;
//        ticks =0;
//        int ropeActions = 0;
//
//        for (int i= 0; i<evalTimes; i++){
//            CaveGameState gameState = new CaveGameState().setParams(caveParams).setup();
//            int tmpHeight = 0; // used for accumulating height data
//
//            //StatSummary actionTimes = new StatSummary("Decision time stats");
//            // gameState.setSoundEnabled(true);
//
//            while (!gameState.isTerminal()) {
//                // get the action from the player, update the game state, and show a view
//                ElapsedTimer t = new ElapsedTimer();
//                int action = player.getAction(gameState.copy(), 0);
//
//                int[] actions = new int[]{action};
//                gameState.next(actions);
//
//                // update every game tick
//                tmpHeight += gameState.avatar.s.y;
//
//                if (actions[0] == 1)
//                    ropeActions++;
//            }
//
//            // update after every game
//            scoreList.add((double)gameState.getScore());
//            ticksList.add((double)gameState.nTicks);
//            heightList.add((double)(tmpHeight/gameState.nTicks));
//            avgSpeedList.add((double)(caveParams.width/gameState.nTicks));
//            finalPosList.add( (double)gameState.avatar.s.x);
//
//            anchorList.add((((double)gameState.connectionCount/(double)caveParams.nAnchors) /(double)gameState.nTicks));
//            System.out.println((int) gameState.getScore());
//
//            //anchorNormalized = gameState.connectionCount;
//        }
//
//
//        //int anc = caveParams.nAnchors;
//        //anchorNormalized = takeListMean((anchorList));
//
//
//
//        anchorNormalized = takeListMean(anchorList);
//        score = takeListMean(scoreList);
//        ticks = takeListMean(ticksList);
//        height = takeListMean(heightList);
//        averageSpeed = takeListMean(avgSpeedList);
//        ropeActions = ropeActions/evalTimes;
//
//        // add result into the correct format
//        Results res = new Results();
//        res.game_score=score;
//        java.util.Map<java.lang.String,java.lang.Double> behaviourMap = new java.util.HashMap<>();
//        behaviourMap.put("height", height);
//        behaviourMap.put("averageSpeed", averageSpeed);
//        behaviourMap.put("ticks", ticks);
//        behaviourMap.put("ropeActions", (double)ropeActions);
//        behaviourMap.put("anchorNormalized", anchorNormalized);
//        res.behaviour = behaviourMap;
//
//        return res;
//
//
//    }

    public static Results PlayGame(java.util.Map<java.lang.String,java.lang.Double> params){
        CaveGameState gameState;
        // use fixed RHEA agent
        int nEvals = 5;
        int seqLength = 100;
        boolean useShiftBuffer = true;

        // initialize stats
        double height = 0;
        double averageSpeed = 0;
        double score = 0;
        double ticks = 0;
        int ropeActions = 0;

        // Reinitialize arrays every time
        ArrayList<Double> heightList = new ArrayList<>();
        ArrayList<Double> avgSpeedList = new ArrayList<>();
        ArrayList<Double> scoreList = new ArrayList<>();
        ArrayList<Double> ticksList = new ArrayList<>();
        ArrayList<Double> anchorList = new ArrayList<>();
        ArrayList<Double> finalPosList = new ArrayList<>();


        // get agent and game from the given parameters
        EvoAgent player = getEvoAgentFromFactory(nEvals, seqLength, useShiftBuffer);
        CaveSwingParams caveParams = setParams(params);

        // main loop for evaluating games
        for (int i= 0; i < evalTimes; i++){
            gameState = new CaveGameState().setParams(caveParams).setup();
            // run main game loop
            while (!gameState.isTerminal()) {
                // get the action from the player, update the game state, and visualize it
                int action = player.getAction(gameState.copy(), 0);
                int[] actions = new int[]{action};
                if (actions[0] == 1)
                    ropeActions++;
                gameState.next(actions);

                // stats at every single game frame
                height += gameState.avatar.s.y;
                if (actions[0] == 1)
                    ropeActions++;
            }

            // update after every game
            scoreList.add((double)gameState.getScore());
            ticksList.add((double)gameState.nTicks);
            heightList.add((double)(height/gameState.nTicks));
            avgSpeedList.add((double)(caveParams.width/gameState.nTicks));
            finalPosList.add( (double)gameState.avatar.s.x);
            height = 0;

            anchorList.add((((double)gameState.connectionCount/(double)caveParams.nAnchors) /(double)gameState.nTicks));
            System.out.println((int) gameState.getScore());
            anchorNormalized = gameState.connectionCount;
        }


        // collect stats
        // TODO you can add new behaviour descriptors here
        anchorNormalized = takeListMean(anchorList);
        score = takeListMean(scoreList);
        ticks = takeListMean(ticksList);
        height = takeListMean(heightList);
        averageSpeed = takeListMean(avgSpeedList);

        // add result into the correct format
        Results res = new Results();
        res.game_score=score;
        java.util.Map<java.lang.String,java.lang.Double> behaviourMap = new java.util.HashMap<String,Double>();
        behaviourMap.put("height", height);
        behaviourMap.put("averageSpeed", averageSpeed);
        behaviourMap.put("ticks", (double)ticks);
        behaviourMap.put("ropeActions", (double)ropeActions);
        behaviourMap.put("anchorNormalized", anchorNormalized);
        res.behaviour = behaviourMap;

        return res;
    }

    public static Results testParams(java.util.Map<java.lang.String,java.lang.Double> params){
        CaveSwingParams caveParams = setParams(params);
        EvoAgent player = getEvoAgentFromFactory(params);

        boolean showEvolution = true;
        int frameDelay = 50;

        double height = 0;
        double averageSpeed =0;
        double score =0;
        int ticks =0;

        System.out.println("playing game");
        CaveGameState gameState = new CaveGameState().setParams(caveParams).setup();
        CaveView view = new CaveView().setGameState(gameState).setParams(caveParams);
        view.scrollView = true;
        view.scrollWidth = 800;

        String title = "Evo Agent Visual GVGAISimpleTest";
        JEasyFrame frame = new JEasyFrame(view, title);
        if (showEvolution) frame.setLocation(0, 350);
        try {
            ViewUtil.waitUntilReady(view);

        } catch (Exception e){
            System.out.println("error");
        }

        StatSummary actionTimes = new StatSummary("Decision time stats");
        // gameState.setSoundEnabled(true);
        while (!gameState.isTerminal()) {
            // get the action from the player, update the game state, and show a view


            ElapsedTimer t = new ElapsedTimer();
            int action = player.getAction(gameState.copy(), 0);
            // recall the action array is needed for generality for n-player games
            actionTimes.add(t.elapsed());

            int[] actions = new int[]{action};
            gameState.next(actions);
            height += gameState.avatar.s.y;

            CaveGameState viewState = ((CaveGameState) gameState.copy());

            view.playouts = player.evoAlg.getLogger().solutions;
            view.setGameState(viewState).repaint();
            frame.setTitle(title + " : " + gameState.nTicks + " : " + gameState.isTerminal() + " : " + (int) gameState.getScore());
            try {Thread.sleep(frameDelay);
            }
            catch (Exception e)
            {
                System.out.println(e);
            }
        }

        frame.dispose();
        score = gameState.getScore();
        ticks = gameState.nTicks;
        height /= ticks;
        averageSpeed = caveParams.width/ticks;
        System.out.println(actionTimes);
        System.out.println((int) gameState.getScore());
        System.out.println("number of ropes used  " + gameState.connectionCount);

        return new Results();
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

    public static EvoAgent getEvoAgentFromFactory(java.util.Map<java.lang.String,java.lang.Double> params) {

        EvoAgentFactory factory = new EvoAgentFactory();
        factory.nEvals = params.get("nEvals").intValue();
        factory.seqLength = params.get("seqLength").intValue();
        factory.useShiftBuffer = (params.get("useShiftBuffer").intValue() == 1) ? true: false;
        factory.totalRandomMutation = (params.get("totalRandomMutation").intValue() == 1) ? true: false;
        factory.mutationRate = params.get("mutationRate").intValue();
        return factory.getAgent();
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

    public static double takeListMean(ArrayList<Double> lst){
        double result = 0;
        for (Double d: lst){
            result+= d;
        }
        return result/lst.size();
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