exports.Verbs = Verbs;

let util = require('../util');

function Verbs() {
  // wordId:success:failure:consecutive:ring,...
  let statsData = localStorage.getItem('verbs');

  let learningVerbs = [], learnedVerbs = [], restVerbs = [];
  let verbsStats = new Map();

  if (statsData) {
    statsData.split(',').forEach(x => {
      let data = x.split(':'),
          id = parseInt(data[0], 10),
          success = parseInt(data[1], 10),
          failures = parseInt(data[2], 10),
          consecutive = parseInt(data[3], 10);

      if (isNaN(id)) return;
      
      isNaN(success) && (success = 0, failures = 0);
      isNaN(failures) && (success = 0, failures = 0);
      isNaN(consecutive) && (consecutive = 0);

      switch (data[4]) {
      case '0': restVerbs.push(id); break;
      case '1': learnedVerbs.push(id); break;
      case '2': learningVerbs.push(id); break;
      default: restVerbs.push(id);
      }

      verbsStats.set(id, {
        success: success,
        failures: failures,
        consecutive: consecutive
      });
    });
  } else {
    for (let i = 0; i < 10; i++) learningVerbs.push(i);
    for (let i = 10; i < VERBS_DATA.length; i++) restVerbs.push(i);
  }

  let totalSize = learningVerbs.length + learnedVerbs.length + restVerbs.length;
  for (let i = totalSize; i < VERBS_DATA.length; i++) {
    restVerbs.push(i);
  }

  saveStats();

  function saveStats() {
    let sb = [];

    learningVerbs.forEach(addStat.bind(null, 2));
    learnedVerbs.forEach(addStat.bind(null, 1));
    restVerbs.forEach(addStat.bind(null, 0));

    let data = sb.join(',');

    localStorage.setItem('verbs', data);

    function addStat(ring, id) {
      let stat = verbsStats.get(id);

      if (!stat) stat = {success: 0, failures: 0, consecutive: 0};

      sb.push(`${id}:${stat.success}:${stat.failures}:${stat.consecutive}:${ring}`);
    }
  }

  function getRandomWord() {
    let wordId;

    let rand = Math.random();
    if (rand < 0.7) wordId = learningVerbs[Math.random() * learningVerbs.length | 0];
    else if (rand < 0.9 && learnedVerbs.length > 0) wordId = learnedVerbs[Math.random() * learnedVerbs.length | 0];
    else wordId = restVerbs[Math.random() * restVerbs.length | 0];

    let wordData = VERBS_DATA[wordId];

    let wordText = wordData[0],
        wrongSecondForms = wordData[1].slice(1),
        wrongThirdForms = wordData[2].slice(1);

    let choices = [],
        choicesSet = new Set();

    function tryAddToChoices(x) {
      if (choicesSet.has(x)) return;

      choices.push({id: 'incorrect' + choices.length, value: x});
      choicesSet.add(x);
    }

    // put all wrong choices
    wrongSecondForms.forEach(secondForm => {
      wrongThirdForms.forEach(thirdForm => {
        tryAddToChoices(secondForm + ' / ' + thirdForm);
      });
    });

    if (choices.length < 4) tryAddToChoices(addEd(wordText) + ' / ' + addEd(wordText));
    if (choices.length < 4) tryAddToChoices(addEd(wordText) + ' / ' + addEn(wordText));

    while (choices.length < 4) {
      let rand = Math.random();

      if (rand < 0.4) tryAddToChoices(changeRandomVovel(wordData[1][0]) + ' / ' + wordData[2][0]);
      else if (rand < 0.8) tryAddToChoices(wordData[1][0] + ' / ' + changeRandomVovel(wordData[2][0]));
      else tryAddToChoices(changeRandomVovel(wordData[1][0]) + ' / ' + changeRandomVovel(wordData[2][0]));
    }

    // select 3 random choices
    util.shuffleArrayInPlace(choices);
    choices = choices.slice(0, 3);

    choices.push({id: 'correct', value: wordData[1][0] + ' / ' + wordData[2][0]});
    util.shuffleArrayInPlace(choices);

    let translation = TRANSLATIONS.get(wordText);
    if (translation) wordText = wordText + ' (' + translation + ')';

    return {
      wordId: wordId,
      word: wordText,
      choices: choices
    };
  }

  this.getRandomWord = function() {
    return Promise.resolve(getRandomWord());
  };

  this.checkWordAndGetNextRandomWord = function(wordId, choiceId) {
    let stats = verbsStats.get(wordId);
    if (stats) {
      if (choiceId === 'correct') {
        stats.success++;
        stats.consecutive++;

        if (stats.consecutive > 5) {
          let pos = learningVerbs.indexOf(wordId),
              randRestPos = Math.random() * restVerbs.length | 0;

          if (pos !== -1) {
            learningVerbs.splice(pos, 1);
            learnedVerbs.push(wordId);

            let restWordId = restVerbs[randRestPos];
            restVerbs.splice(randRestPos, 1);
            learningVerbs.push(restWordId);

            let stats = verbsStats.get(restWordId);
            if (stats) stats.consecutive = 0;
          } else if (stats.consecutive > 10) {
            pos = learnedVerbs.indexOf(wordId);

            if (pos !== -1) {
              learnedVerbs.splice(pos, 1);
              restVerbs.push(wordId);

              stats.consecutive = 0;
            }
          }
        }
      } else {
        stats.failures++;
        stats.consecutive = 0;

        let pos = learnedVerbs.indexOf(wordId);
        
        if (pos !== -1) {
          learnedVerbs.splice(pos, 1);

          learningVerbs.sort(function(a, b) {
            let aStats = verbsStats.get(a),
                bStats = verbsStats.get(b);

            if (aStats && bStats) {
              return bStats.success - aStats.success;
            }
          });
          learningVerbs.splice(learningVerbs.length - 1, 1);

          learningVerbs.push(wordId);
        }
      }
    } else {
      verbsStats.set(wordId, {
        success: choiceId === 'correct' ? 1 : 0,
        failures: choiceId === 'correct' ? 0 : 1,
        consecutive: choiceId === 'correct' ? 1 : 0
      });
    }
    console.log(stats);

    saveStats();

    return Promise.resolve({
      correct: 'correct',
      word: getRandomWord()
    });
  };
}

const vovelsSet = {'a': true, 'o': true, 'i': true,  'e': true, 'u': true, 'y': true};

function isEndsByCVC(word) {
  if (word.length < 3) return !(word[word.length - 1] in vovelsSet);

  if (word[word.length - 1] in vovelsSet) return false;
  if (!(word[word.length - 2] in vovelsSet)) return false;
  if (word[word.length - 3] in vovelsSet) return false;

  return true;
}

function addEd(word) {
  let lastChar = word[word.length - 1];

  if (isEndsByCVC(word) && lastChar !== 'w') {
    // TODO: take into account stress of syllable
    return word + lastChar + 'ed';
  } else if (lastChar === 'y') {
    return word.slice(0, word.length - 1) + 'ied';
  } else if (lastChar === 'e') {
    return word + 'd';
  } else {
    return word + 'ed';
  }
}
function addEn(word) {
  // actually there are such thing as "add -en"

  let lastChar = word[word.length - 1];

  if (isEndsByCVC(word) && lastChar !== 'w') {
    // TODO: take into account stress of syllable
    return word + lastChar + 'en';
  } else if (lastChar === 'e') {
    return word + 'n';
  } else {
    return word + 'en';
  }
}

function changeRandomVovel(word) {
  const vovelsList = ['a', 'o', 'i', 'e', 'u', 'y'];

  let vovelPositions = [];

  for (let i = 0; i < word.length; i++) {
    if (word[i] in vovelsSet) vovelPositions.push(i);
  }

  let randPos = vovelPositions[Math.random() * vovelPositions.length | 0],
      randVovel;

  do {
    randVovel = vovelsList[Math.random() * vovelsList.length | 0];
  } while (randVovel === word[randPos]);

  return word.slice(0, randPos) + randVovel + word.slice(randPos + 1);
}

const VERBS_DATA = [['awake', ['awoke'], ['awoken']],
['be', ['was/were'], ['been']],
['bear', ['bore'], ['born']],
['beat', ['beat'], ['beat']],
['become', ['became'], ['become']],
['begin', ['began'], ['begun']],
['bend', ['bent'], ['bent']],
['beset', ['beset'], ['beset']],
['bet', ['bet'], ['bet']],
['bid', ['bid'], ['bid']],
['bind', ['bound'], ['bound']],
['bite', ['bit'], ['bitten']],
['bleed', ['bled'], ['bled']],
['blow', ['blew'], ['blown']],
['break', ['broke'], ['broken']],
['breed', ['bred'], ['bred']],
['bring', ['brought'], ['brought']],
['broadcast', ['broadcast'], ['broadcast']],
['build', ['built'], ['built']],
['burn', ['burnt'], ['burnt']],
['burst', ['burst'], ['burst']],
['buy', ['bought'], ['bought']],
['cast', ['cast'], ['cast']],
['catch', ['caught'], ['caught']],
['choose', ['chose'], ['chosen']],
['cling', ['clung'], ['clung']],
['come', ['came'], ['come']],
['cost', ['cost'], ['cost']],
['creep', ['crept'], ['crept']],
['cut', ['cut'], ['cut']],
['deal', ['dealt'], ['dealt']],
['dig', ['dug'], ['dug']],
['dive', ['dove'], ['dived']],
['do', ['did'], ['done']],
['draw', ['drew'], ['drawn']],
['dream', ['dreamt'], ['dreamt']],
['drive', ['drove'], ['driven']],
['drink', ['drank'], ['drunk']],
['eat', ['ate'], ['eaten']],
['fall', ['fell'], ['fallen']],
['feed', ['fed'], ['fed']],
['feel', ['felt'], ['felt']],
['fight', ['fought'], ['fought']],
['find', ['found'], ['found']],
['fit', ['fit'], ['fit']],
['flee', ['fled'], ['fled']],
['fling', ['flung'], ['flung']],
['fly', ['flew'], ['flown']],
['forbid', ['forbade'], ['forbidden']],
['forget', ['forgot'], ['forgotten']],
['forego', ['forewent'], ['foregone']],
['forgo', ['forewent'], ['foregone']],
['forgive', ['forgave'], ['forgiven']],
['forsake', ['forsook'], ['forsaken']],
['freeze', ['froze'], ['frozen']],
['get', ['got'], ['gotten']],
['give', ['gave'], ['given']],
['go', ['went'], ['gone']],
['grind', ['ground'], ['ground']],
['grow', ['grew'], ['grown']],
['hang', ['hung'], ['hung']],
['hear', ['heard'], ['heard']],
['hide', ['hid'], ['hidden']],
['hit', ['hit'], ['hit']],
['hold', ['held'], ['held']],
['hurt', ['hurt'], ['hurt']],
['is', ['was'], ['were']],
['keep', ['kept'], ['kept']],
['kneel', ['knelt'], ['knelt']],
['knit', ['knit'], ['knit']],
['know', ['knew'], ['know']],
['lay', ['laid'], ['laid']],
['lead', ['led'], ['led']],
['leap', ['lept'], ['lept']],
['learn', ['learned'], ['learned']],
['leave', ['left'], ['left']],
['lend', ['lent'], ['lent']],
['let', ['let'], ['let']],
['lie', ['lay'], ['lain']],
['light', ['lit'], ['lighted']],
['lose', ['lost'], ['lost']],
['make', ['made'], ['made']],
['mean', ['meant'], ['meant']],
['meet', ['met'], ['met']],
['misspell', ['misspelt'], ['misspelt']],
['mistake', ['mistook'], ['mistaken']],
['mow', ['mowed'], ['mowed']],
['overcome', ['overcame'], ['overcome']],
['overdo', ['overdid'], ['overdone']],
['overtake', ['overtook'], ['overtaken']],
['overthrow', ['overthrew'], ['overthrown']],
['pay', ['paid'], ['paid']],
['plead', ['pled'], ['pled']],
['prove', ['proved'], ['proved']],
['put', ['put'], ['put']],
['quit', ['quit'], ['quit']],
['read', ['read'], ['read']],
['rid', ['rid'], ['rid']],
['ride', ['rode'], ['ridden']],
['ring', ['rang'], ['rung']],
['rise', ['rose'], ['risen']],
['run', ['ran'], ['run']],
['saw', ['sawed'], ['sawn']],
['say', ['said'], ['said']],
['see', ['saw'], ['seen']],
['seek', ['sought'], ['sought']],
['sell', ['sold'], ['sold']],
['send', ['sent'], ['sent']],
['set', ['set'], ['set']],
['sew', ['sewed'], ['sewn']],
['shake', ['shook'], ['shaken']],
['shave', ['shaved'], ['shaven']],
['shear', ['shore'], ['shorn']],
['shed', ['shed'], ['shed']],
['shine', ['shone'], ['shone']],
['shoe', ['shoed'], ['shod']],
['shoot', ['shot'], ['shot']],
['show', ['showed'], ['shown']],
['shrink', ['shrank'], ['shrunk']],
['shut', ['shut'], ['shut']],
['sing', ['sang'], ['sung']],
['sink', ['sank'], ['sunk']],
['sit', ['sat'], ['sat']],
['sleep', ['slept'], ['slept']],
['slay', ['slew'], ['slain']],
['slide', ['slid'], ['slid']],
['sling', ['slung'], ['slung']],
['slit', ['slit'], ['slit']],
['smite', ['smote'], ['smitten']],
['sow', ['sowed'], ['sown']],
['speak', ['spoke'], ['spoken']],
['speed', ['sped'], ['sped']],
['spend', ['spent'], ['spent']],
['spill', ['spilled'], ['spilled']],
['spin', ['spun'], ['spun']],
['spit', ['spat'], ['spit']],
['split', ['split'], ['split']],
['spread', ['spread'], ['spread']],
['spring', ['sprang'], ['sprung']],
['stand', ['stood'], ['stood']],
['steal', ['stole'], ['stolen']],
['stick', ['stuck'], ['stuck']],
['sting', ['stung'], ['stung']],
['stink', ['stank'], ['stunk']],
['stride', ['strod'], ['stridden']],
['strike', ['struck'], ['struck']],
['string', ['strung'], ['strung']],
['strive', ['strove'], ['striven']],
['swear', ['swore'], ['sworn']],
['sweep', ['swept'], ['swept']],
['swell', ['swelled'], ['swollen']],
['swim', ['swam'], ['swum']],
['swing', ['swung'], ['swung']],
['take', ['took'], ['taken']],
['teach', ['taught'], ['taught']],
['tear', ['tore'], ['torn']],
['tell', ['told'], ['told']],
['think', ['thought'], ['thought']],
['thrive', ['thrived'], ['thrived']],
['throw', ['threw'], ['thrown']],
['thrust', ['thrust'], ['thrust']],
['tread', ['trod'], ['trodden']],
['understand', ['understood'], ['understood']],
['uphold', ['upheld'], ['upheld']],
['upset', ['upset'], ['upset']],
['wake', ['woke'], ['woken']],
['wear', ['wore'], ['worn']],
['weave', ['wove'], ['woven']],
['wed', ['wed'], ['wed']],
['weep', ['wept'], ['wept']],
['wind', ['wound'], ['wound']],
['win', ['won'], ['won']],
['withhold', ['withheld'], ['withheld']],
['withstand', ['withstood'], ['withstood']],
['wring', ['wrung'], ['wrung']],
['write', ['wrote'], ['written']]];

const TRANSLATIONS_DATA = [['be','быть, являться'],['beat','бить, колотить'],['become','становиться'],['begin','начинать'],['bend','гнуть'],['bet','держать пари'],['bite','кусать'],['blow','дуть, выдыхать'],['break','ломать, разбивать, разрушать'],['bring','приносить, привозить, доставлять'],['build','строить, сооружать'],['buy','покупать, приобретать'],['catch','ловить, поймать, схватить'],['choose','выбирать, избирать'],['come','приходить, подходить'],['cost','стоить, обходиться'],['cut','резать, разрезать'],['deal','иметь дело, распределять'],['dig','копать, рыть'],['do','делать, выполнять'],['draw','рисовать, чертить'],['drink','пить'],['drive','ездить, подвозить'],['eat','есть, поглощать, поедать'],['fall','падать'],['feed','кормить'],['feel','чувствовать, ощущать'],['fight','драться, сражаться, воевать'],['find','находить, обнаруживать'],['fly','летать'],['forget','забывать о (чём-либо)'],['forgive','прощать'],['freeze','замерзать, замирать'],['get','получать, добираться'],['give','дать, подать, дарить'],['go','идти, двигаться'],['grow','расти, вырастать'],['hang','вешать, развешивать, висеть'],['have','иметь, обладать'],['hear','слышать, услышать'],['hide','прятать, скрывать'],['hit','ударять, поражать'],['hold','держать, удерживать, задерживать'],['hurt','ранить, причинять боль, ушибить'],['keep','хранить, сохранять, поддерживать'],['know','знать, иметь представление'],['lay','класть, положить, покрывать'],['lead','вести за собой, сопровождать, руководить'],['leave','покидать, уходить, уезжать, оставлять'],['lend','одалживать, давать взаймы (в долг)'],['let','позволять, разрешать'],['lie','лежать'],['light','зажигать, светиться, освещать'],['lose','терять, лишаться, утрачивать'],['make','делать, создавать, изготавливать'],['mean','значить, иметь в виду, подразумевать'],['meet','встречать, знакомиться'],['pay','платить, оплачивать, рассчитываться'],['put','ставить, помещать, класть'],['read','читать, прочитать'],['ride','ехать верхом, кататься'],['ring','звенеть, звонить'],['rise','восходить, вставать, подниматься'],['run','бежать, бегать'],['say','говорить, сказать, произносить'],['see','видеть'],['seek','искать, разыскивать'],['sell','продавать, торговать'],['send','посылать, отправлять, отсылать'],['set','устанавливать, задавать, назначать'],['shake','трясти, встряхивать'],['shine','светить, сиять, озарять'],['shoot','стрелять'],['show','показывать'],['shut','закрывать, запирать, затворять'],['sing','петь, напевать'],['sink','тонуть, погружаться'],['sit','сидеть, садиться'],['sleep','спать'],['speak','говорить, разговаривать, высказываться'],['spend','тратить, расходовать, проводить (время)'],['stand','стоять'],['steal','воровать, красть'],['stick','втыкать, приклеивать'],['strike','ударять, бить, поражать'],['swear','клясться, присягать'],['sweep','мести, подметать, смахивать'],['swim','плавать, плыть'],['swing','качаться, вертеться'],['take','брать, хватать, взять'],['teach','учить, обучать'],['tear','рвать, отрывать'],['tell','рассказывать'],['think','думать, мыслить, размышлять'],['throw','бросать, кидать, метать'],['understand','понимать, постигать'],['wake','просыпаться, будить'],['wear','носить (одежду)'],['win','победить, выиграть'],['write','писать, записывать']];

const TRANSLATIONS = new Map();
TRANSLATIONS_DATA.forEach(([word, translation]) => {
  TRANSLATIONS.set(word, translation);
});
