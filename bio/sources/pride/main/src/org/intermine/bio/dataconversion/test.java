package org.intermine.bio.dataconversion;

class test {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Hello to test demo");
		
		PridePeptideData data = new PridePeptideData("IL",0,1);
		
		String seq = "MKFEDLLATNKQVQFAHAATQHYKSVKTPDFLEKDPHHKKFHNADGLNQQGSSTPST" +
				"ATDANAASTASTHTNTTTFKRHIVAVDDISKMNYEMIKNSPGNVITNANQDEIDISTLKTRLYKDN" +
				"LYAMNDNFLQAVNDQIVTLNAAEQDQETEDPDLSDDEKIDILTKIQENLLEEYQKLSQKERKWFIL" +
				"KELLLDANVELDLFSNRGRKASHPIAFGAVAIPTNVNANSLAFNRTKRRKINKNGLLENIL";
		
		PrideCalculatePos cPos = new PrideCalculatePos(seq, data);
		
		while(cPos.hasNext()) {
			PridePeptideData test = cPos.next();
			System.out.println("Start: " + test.getStartPos());
			System.out.println("End: " + test.getEndPos());
			cPos.remove();
		}
			
		
		/*PrideExpression exp = new PrideExpression("SWISSPROT:DOME_EVE SWISSPROT:Q9ULV4.1 SWISSPROT:P00761.1 SWISSPROT:TRYP_PIG SWISSPROT:T1LLL1.9");
		
		String[] test = new String[exp.accessionCounter];
		String[] test1 = new String[exp.identifierCounter];
		
			test = exp.getAccession();
			test1 = exp.getIdentifier();

		
	
		for(int i=0; i < test.length; i++) {
			System.out.println("AccessionIds: " + test[i]);
		}
		for(int i=0; i < test1.length; i++)
			System.out.println("IdentifierIds: " + test1[i]);*/
	
	}

}