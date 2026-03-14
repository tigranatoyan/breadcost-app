const fs = require('fs');
let content = fs.readFileSync('frontend/locales/hy.ts', 'utf8');

// Find the productionPlans section and add shift keys after yieldPlaceholder line
const yieldIdx = content.indexOf('yieldPlaceholder:');
if (yieldIdx > 0) {
  // Find the end of this line
  const lineEnd = content.indexOf('\n', yieldIdx);
  const afterLine = content.substring(lineEnd + 1);
  const beforeLine = content.substring(0, lineEnd + 1);
  
  const newKeys = 
    "    notesPlaceholder: '\\u0531\\u0580\\u057f\\u0561\\u0564\\u0580\\u0561\\u056f\\u0561\\u0576 \\u0569\\u056b\\u0574\\u056b \\u0570\\u0561\\u0574\\u0561\\u0580 \\u0581\\u0578\\u0582\\u0581\\u0578\\u0582\\u0574\\u0576\\u0565\\u0580 (\\u0578\\u0579 \\u057a\\u0561\\u0580\\u057f\\u0561\\u0564\\u056b\\u0580)',\r\n" +
    "    workOrderCount: '{count} \\u0561\\u0577\\u056d\\u0561\\u057f\\u0561\\u0576\\u0584\\u0561\\u0575\\u056b\\u0576 \\u057a\\u0561\\u057f\\u057e\\u0565\\u0580',\r\n" +
    "    shifts: {\r\n" +
    "      MORNING: '\\u0531\\u057c\\u0561\\u057e\\u0578\\u057f',\r\n" +
    "      AFTERNOON: '\\u0551\\u0565\\u0580\\u0565\\u056f\\u0578\\u0575\\u0561\\u0576',\r\n" +
    "      NIGHT: '\\u0533\\u056b\\u0577\\u0565\\u0580\\u0561\\u0575\\u056b\\u0576',\r\n" +
    "    },\r\n";
  
  content = beforeLine + newKeys + afterLine;
  console.log('Added shift/notes keys after yieldPlaceholder');
} else {
  console.log('yieldPlaceholder not found');
}

fs.writeFileSync('frontend/locales/hy.ts', content, 'utf8');
console.log('Done');
