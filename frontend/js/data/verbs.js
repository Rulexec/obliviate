exports.Verbs = Verbs;

let util = require('../util');

function Verbs() {
  function getRandomWord() {
    let wordId = Math.random() * VERBS_DATA.length | 0,
        wordData = VERBS_DATA[wordId];

    let isFromFirstForm = Math.random() < 0.9,
        toForm = 2 + (Math.random() * 2 | 0);

    let wordText, correctText;

    if (isFromFirstForm) {
      wordText = wordData[0] + ' → ' + (toForm === 2 ? 'II' : 'III');
      correctText = wordData[toForm - 1];
    } else {
      wordText = wordData[toForm - 1] + ' → I';
      correctText = wordData[0];
    }

    let choices = [],
        choicesSet = new Set();

    choices.push({id: 'correct', value: correctText});
    choicesSet.add(correctText);

    function tryAddToChoices(x) {
      if (choicesSet.has(x)) return;

      choices.push({id: 'incorrect' + choices.length, value: x});
      choicesSet.add(x);
    }

    wordData.forEach(tryAddToChoices);

    const DOUBLE_LAST = {'t': true, 'g': true},
          PLUS_E = {'d': true, 'k': true};

    if (choices.length < 4) {
      // try add -ed
      let word = wordData[0],
          lastChar = word[word.length - 1];
      if (lastChar in DOUBLE_LAST) {
        tryAddToChoices(word + lastChar + 'ed');
      } else if (lastChar in PLUS_E) {
        tryAddToChoices(word + 'ed');
      } else {
        tryAddToChoices(word + 'd');
      }
    }
    if (choices.length < 4) {
      // try add -en
      let word = wordData[0],
          lastChar = word[word.length - 1];
      if (lastChar in DOUBLE_LAST) {
        tryAddToChoices(word + lastChar + 'en');
      } else {
        tryAddToChoices(word + 'en');
      }
    }
    
    const vovelsList = ['a', 'o', 'i', 'e', 'u', 'y'],
          vovelsSet = {'a': true, 'o': true, 'i': true,  'e': true, 'u': true, 'y': true};

    while (choices.length < 4) {
      let word = wordData[Math.random() * 3 | 0];

      let vovelPositions = [];

      for (let i = 0; i < word.length; i++) {
        if (word[i] in vovelsSet) vovelPositions.push(i);
      }

      let randPos = vovelPositions[Math.random() * vovelPositions.length | 0],
          randVovel;

      do {
        randVovel = vovelsList[Math.random() * vovelsList.length | 0];
      } while (randVovel === word[randPos]);

      tryAddToChoices(word.slice(0, randPos) + randVovel + word.slice(randPos + 1));
    }

    util.shuffleArrayInPlace(choices);

    return {
      wordId: wordId,
      word: wordText,
      choices: choices
    };
  }

  this.getRandomWord = function() {
    return Promise.resolve(getRandomWord());
  };

  this.checkWordAndGetNextRandomWord = function() {
    return Promise.resolve({
      correct: 'correct',
      word: getRandomWord()
    });
  };
}

let VERBS_DATA = [['awake', 'awoke', 'awoken'],
['be', 'was/were', 'been'],
['bear', 'bore', 'born'],
['beat', 'beat', 'beat'],
['become', 'became', 'become'],
['begin', 'began', 'begun'],
['bend', 'bent', 'bent'],
['beset', 'beset', 'beset'],
['bet', 'bet', 'bet'],
['bid', 'bid', 'bid'],
['bind', 'bound', 'bound'],
['bite', 'bit', '', 'bitten'],
['bleed', 'bled', 'bled'],
['blow', 'blew', 'blown'],
['break', 'broke', 'broken'],
['breed', 'bred', 'bred'],
['bring', 'brought', 'brought'],
['broadcast', 'broadcast', 'broadcast'],
['build', 'built', 'built'],
['burn', 'burnt', 'burnt'],
['burst', 'burst', 'burst'],
['buy', 'bought', 'bought'],
['cast', 'cast', 'cast'],
['catch', 'caught', 'caught'],
['choose', 'chose', 'chosen'],
['cling', 'clung', 'clung'],
['come', 'came', 'come'],
['cost', 'cost', 'cost'],
['creep', 'crept', 'crept'],
['cut', 'cut', 'cut'],
['deal', 'dealt', 'dealt'],
['dig', 'dug', 'dug'],
['dive', 'dove', 'dived'],
['do', 'did', 'done'],
['draw', 'drew', 'drawn'],
['dream', 'dreamt', 'dreamt'],
['drive', 'drove', 'driven'],
['drink', 'drank', 'drunk'],
['eat', 'ate', 'eaten'],
['fall', 'fell', 'fallen'],
['feed', 'fed', 'fed'],
['feel', 'felt', 'felt'],
['fight', 'fought', 'fought'],
['find', 'found', 'found'],
['fit', 'fit', 'fit'],
['flee', 'fled', 'fled'],
['fling', 'flung', 'flung'],
['fly', 'flew', 'flown'],
['forbid', 'forbade', 'forbidden'],
['forget', 'forgot', 'forgotten'],
['forego', 'forewent', 'foregone'],
['forgo', 'forewent', 'foregone'],
['forgive', 'forgave', 'forgiven'],
['forsake', 'forsook', 'forsaken'],
['freeze', 'froze', 'frozen'],
['get', 'got', 'gotten'],
['give', 'gave', 'given'],
['go', 'went', 'gone'],
['grind', 'ground', 'ground'],
['grow', 'grew', 'grown'],
['hang', 'hung', 'hung'],
['hear', 'heard', 'heard'],
['hide', 'hid', 'hidden'],
['hit', 'hit', 'hit'],
['hold', 'held', 'held'],
['hurt', 'hurt', 'hurt'],
['is', 'was', 'were'],
['keep', 'kept', 'kept'],
['kneel', 'knelt', 'knelt'],
['knit', 'knit', 'knit'],
['know', 'knew', 'know'],
['lay', 'laid', 'laid'],
['lead', 'led', 'led'],
['leap', 'lept', 'lept'],
['learn', 'learned', 'learned'],
['leave', 'left', 'left'],
['lend', 'lent', 'lent'],
['let', 'let', 'let'],
['lie', 'lay', 'lain'],
['light', 'lit', 'lighted'],
['lose', 'lost', 'lost'],
['make', 'made', 'made'],
['mean', 'meant', 'meant'],
['meet', 'met', 'met'],
['misspell', 'misspelt', 'misspelt'],
['mistake', 'mistook', 'mistaken'],
['mow', 'mowed', 'mowed'],
['overcome', 'overcame', 'overcome'],
['overdo', 'overdid', 'overdone'],
['overtake', 'overtook', 'overtaken'],
['overthrow', 'overthrew', 'overthrown'],
['pay', 'paid', 'paid'],
['plead', 'pled', 'pled'],
['prove', 'proved', 'proved'],
['put', 'put', 'put'],
['quit', 'quit', 'quit'],
['read', 'read', 'read'],
['rid', 'rid', 'rid'],
['ride', 'rode', 'ridden'],
['ring', 'rang', 'rung'],
['rise', 'rose', 'risen'],
['run', 'ran', 'run'],
['saw', 'sawed', 'sawn'],
['say', 'said', 'said'],
['see', 'saw', 'seen'],
['seek', 'sought', 'sought'],
['sell', 'sold', 'sold'],
['send', 'sent', 'sent'],
['set', 'set', 'set'],
['sew', 'sewed', 'sewn'],
['shake', 'shook', 'shaken'],
['shave', 'shaved', 'shaven'],
['shear', 'shore', 'shorn'],
['shed', 'shed', 'shed'],
['shine', 'shone', 'shone'],
['shoe', 'shoed', 'shod'],
['shoot', 'shot', 'shot'],
['show', 'showed', 'shown'],
['shrink', 'shrank', 'shrunk'],
['shut', 'shut', 'shut'],
['sing', 'sang', 'sung'],
['sink', 'sank', 'sunk'],
['sit', 'sat', 'sat'],
['sleep', 'slept', 'slept'],
['slay', 'slew', 'slain'],
['slide', 'slid', 'slid'],
['sling', 'slung', 'slung'],
['slit', 'slit', 'slit'],
['smite', 'smote', 'smitten'],
['sow', 'sowed', 'sown'],
['speak', 'spoke', 'spoken'],
['speed', 'sped', 'sped'],
['spend', 'spent', 'spent'],
['spill', 'spilled', 'spilled'],
['spin', 'spun', 'spun'],
['spit', 'spat', 'spit'],
['split', 'split', 'split'],
['spread', 'spread', 'spread'],
['spring', 'sprang', 'sprung'],
['stand', 'stood', 'stood'],
['steal', 'stole', 'stolen'],
['stick', 'stuck', 'stuck'],
['sting', 'stung', 'stung'],
['stink', 'stank', 'stunk'],
['stride', 'strod', 'stridden'],
['strike', 'struck', 'struck'],
['string', 'strung', 'strung'],
['strive', 'strove', 'striven'],
['swear', 'swore', 'sworn'],
['sweep', 'swept', 'swept'],
['swell', 'swelled', 'swollen'],
['swim', 'swam', 'swum'],
['swing', 'swung', 'swung'],
['take', 'took', 'taken'],
['teach', 'taught', 'taught'],
['tear', 'tore', 'torn'],
['tell', 'told', 'told'],
['think', 'thought', 'thought'],
['thrive', 'thrived', 'thrived'],
['throw', 'threw', 'thrown'],
['thrust', 'thrust', 'thrust'],
['tread', 'trod', 'trodden'],
['understand', 'understood', 'understood'],
['uphold', 'upheld', 'upheld'],
['upset', 'upset', 'upset'],
['wake', 'woke', 'woken'],
['wear', 'wore', 'worn'],
['weave', 'wove', 'woven'],
['wed', 'wed', 'wed'],
['weep', 'wept', 'wept'],
['wind', 'wound', 'wound'],
['win', 'won', 'won'],
['withhold', 'withheld', 'withheld'],
['withstand', 'withstood', 'withstood'],
['wring', 'wrung', 'wrung'],
['write', 'wrote', 'written']];
