"""One reviewed durable-response callback. Historical fixtures remain immutable."""
from pathlib import Path
def strip_promotion(name,text):
    if Path(name).name=="UsageApi.java":
        hook="                BetaGrowthStore.response(context, SubscriptionStore.accountKey(context), usageSnapshot);\n"
        assert text.count(hook)<=1,"Unexpected growth response hook"
        return text.replace(hook,"")
    return text
