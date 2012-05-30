package org.intermine.webservice.server.idresolution;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class IdResolutionService extends JSONService
{
    /**
     * Default constructor.
     * @param im
     */
    public IdResolutionService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        final Input in;
        try {
            in = new Input();
        } catch (JSONException e) {
            throw new BadRequestException("Invalid JSON object", e);
        } catch (IOException e) {
            throw new InternalErrorException("Could not read details", e);
        }
        
        final BagQueryRunner runner = im.getBagQueryRunner();
        
        Job job = new Job(runner, in);

        output.addResultItem(Arrays.asList(job.getUid()));
        
        new Thread(job).run();
    }
    
    @Override
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = super.getHeaderAttributes();
        attributes.put(JSONFormatter.KEY_INTRO, "\"uid\":");
        attributes.put(JSONFormatter.KEY_QUOTE, true);
        return attributes;
    }

    public class Input
    {
        private final List<String> ids;
        private final String extraValue;
        private final String type;
        private final Boolean caseSensitive;
        private final Boolean wildCards;

        Input() throws JSONException, IOException {
            JSONObject requestDetails
                = new JSONObject(new JSONTokener(IdResolutionService.this.request.getReader()));
            JSONArray identifiers = requestDetails.getJSONArray("identifiers");
            ids = new LinkedList<String>();
            for (int i = 0; i < identifiers.length(); i++) {
                ids.add(identifiers.getString(i));
            }
            type = requestDetails.getString("type");
            caseSensitive = requestDetails.optBoolean("caseSensitive", false);
            wildCards = requestDetails.optBoolean("wildCards", true);
            extraValue = requestDetails.optString("extra", null);
        }

        public List<String> getIds() {
            return ids;
        }

        public String getExtraValue() {
            return extraValue;
        }

        public String getType() {
            return type;
        }

        public Boolean getCaseSensitive() {
            return caseSensitive;
        }

        public Boolean getWildCards() {
            return wildCards;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result
                    + ((caseSensitive == null) ? 0 : caseSensitive.hashCode());
            result = prime * result
                    + ((extraValue == null) ? 0 : extraValue.hashCode());
            result = prime * result + ((ids == null) ? 0 : ids.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            result = prime * result
                    + ((wildCards == null) ? 0 : wildCards.hashCode());
            return result;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Input)) {
                return false;
            }
            Input other = (Input) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (caseSensitive == null) {
                if (other.caseSensitive != null) {
                    return false;
                }
            } else if (!caseSensitive.equals(other.caseSensitive)) {
                return false;
            }
            if (extraValue == null) {
                if (other.extraValue != null) {
                    return false;
                }
            } else if (!extraValue.equals(other.extraValue)) {
                return false;
            }
            if (ids == null) {
                if (other.ids != null) {
                    return false;
                }
            } else if (!ids.equals(other.ids)) {
                return false;
            }
            if (type == null) {
                if (other.type != null) {
                    return false;
                }
            } else if (!type.equals(other.type)) {
                return false;
            }
            if (wildCards == null) {
                if (other.wildCards != null) {
                    return false;
                }
            } else if (!wildCards.equals(other.wildCards)) {
                return false;
            }
            return true;
        }

        private IdResolutionService getOuterType() {
            return IdResolutionService.this;
        }
    }


}
