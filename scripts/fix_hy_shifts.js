const fs = require('fs');

let content = fs.readFileSync('frontend/locales/hy.ts', 'utf8');

// 1. Add shift keys to productionPlans section
// Find the yieldPlaceholder line and add after it
const yieldLine = "    yieldPlaceholder: '\u053c\u0580\u0561\u0581\u0576\u0565\u056c \u0562\u0565\u0580\u0584\u0568',";
if (content.includes(yieldLine)) {
  content = content.replace(
    yieldLine,
    yieldLine + "\n" +
    "    notesPlaceholder: '\u0531\u0580\u057f\u0561\u0564\u0580\u0561\u056f\u0561\u0576 \u0569\u056b\u0574\u056b \u0570\u0561\u0574\u0561\u0580 \u0581\u0578\u0582\u0581\u0578\u0582\u0574\u0576\u0565\u0580 (\u0578\u0579 \u057a\u0561\u0580\u057f\u0561\u0564\u056b\u0580)',\n" +
    "    workOrderCount: '{count} \u0561\u0577\u056d\u0561\u057f\u0561\u0576\u0584\u0561\u0575\u056b\u0576 \u057a\u0561\u057f\u057e\u0565\u0580',\n" +
    "    shifts: {\n" +
    "      MORNING: '\u0531\u057c\u0561\u057e\u0578\u057f',\n" +
    "      AFTERNOON: '\u0551\u0565\u0580\u0565\u056f\u0578\u0575\u0561\u0576',\n" +
    "      NIGHT: '\u0533\u056b\u0577\u0565\u0580\u0561\u0575\u056b\u0576',\n" +
    "    },"
  );
  console.log('Added shift keys to productionPlans');
} else {
  console.log('yieldPlaceholder not found in hy.ts');
}

// 2. Add inventory placeholders - find the autoPlan line
const autoPlanLine = "    autoPlan: '\uD83D\uDD04 \u054Bdelays';";
// Try alternative match
if (content.includes('autoPlan')) {
  const autoPlanMatch = content.match(/    autoPlan:.*$/m);
  if (autoPlanMatch) {
    const before = autoPlanMatch[0];
    content = content.replace(
      before,
      "    subtitle: 'FIFO \u056c\u0578\u057f\u0565\u0580, \u057a\u0561\u0570\u0565\u057d\u057f\u056b \u0561\u0570\u0561\u0566\u0561\u0576\u0563\u0576\u0565\u0580, \u0568\u0576\u0564\u0578\u0582\u0576\u0565\u056c\u0578\u0582\u0569\u0575\u0578\u0582\u0576, \u057f\u0565\u0572\u0561\u0583\u0578\u056d\u0574\u0561\u0576 \u0587 \u0573\u0561\u057c\u056b\u0581\u0578\u0582\u0574',\n" +
      "    qtyPlaceholder: '\u0585\u0580. 100',\n" +
      "    costPlaceholder: '\u0585\u0580. 5.50',\n" +
      "    currencyPlaceholder: '\u0585\u0580. USD, EUR',\n" +
      "    exchangeRatePlaceholder: '\u0585\u0580. 12650',\n" +
      "    fromLocationPlaceholder: '\u0585\u0580. RECEIVING',\n" +
      "    toLocationPlaceholder: '\u0585\u0580. PRODUCTION',\n" +
      "    itemNamePlaceholder: '\u0585\u0580. \u0551\u057f\u0561\u057e\u0561\u0580\u056b \u0561\u056c\u0575\u0578\u0582\u0580',\n" +
      "    uomPlaceholder: '\u053f\u0533 / \u0540\u0561\u057f / \u053c',\n" +
      "    adjustQtyPlaceholder: '\u0585\u0580. -5 \u056f\u0561\u0574 +10',\n" +
      "    " + before
    );
    console.log('Added inventory placeholders');
  }
} else {
  console.log('autoPlan not found in hy.ts');
}

fs.writeFileSync('frontend/locales/hy.ts', content, 'utf8');
console.log('hy.ts updated successfully');
