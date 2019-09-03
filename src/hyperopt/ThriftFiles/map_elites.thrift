
namespace java thrift_elites


struct Results {
    1: double game_score,
    2: map<string,double> behaviour
}


service ParamEvaluator
{
    void ping(),
    Results evaluate_params(1:map<string,double> params)
    Results human_play(1:map<string,double> params)
    Results visual_evaluation(1:map<string,double> params)
}