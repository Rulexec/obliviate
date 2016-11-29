let EditReact = require('./edit.jsx').Edit;

exports.Edit = Edit;

function Edit(options) {
  let self = this;

  let render = options.render.bind(options, EditReact),
      dataProvider = options.dataProvider;

  let allWords, index, selectedFilter,
      newWord = null;

  let wordIsValid = x => /^[a-z'\\s]{1,32}$/.test(x),
      translationIsValid = x => /^.{1,255}$/.test(x);

  this.start = function() {
    render({
      isLoading: true,
      index: []
    });

    dataProvider.getAllWordsWithTranslations().then(words => {
      allWords = words;
      index = buildIndex(allWords);

      self.render();
    }, data => {
      if (data.error === '401') options.router.go('');
      else throw new Error(data); // TODO
    });
  };

  this.render = function() {
    let words = allWords;

    if (index.length > 0) {
      if (typeof selectedFilter !== 'number') {
        if (typeof selectedFilter === 'string') {
          let found = index.some((x, i) => {
            if (isWordSatisfiesFilter(selectedFilter, x.value.toLowerCase())) {
              selectedFilter = i;
              return true;
            } else return false;
          });

          if (!found) selectedFilter = 0;
        } else {
          selectedFilter = 0;
        }
      }

      index[selectedFilter].active = true;
      let filter = index[selectedFilter].value.toLowerCase();

      words = allWords.filter(x => {
        return isWordSatisfiesFilter(x.word, filter)
      });

      function isWordSatisfiesFilter(word, filter) {
        return word.slice(0, filter.length).toLowerCase() === filter;
      }
    }

    let wordsCount = words.length;

    let lastWordChangeTimeout = null,
        dictIsShown = false;

    let element = render({
      words: words,
      newWord: newWord,
      index: index,
      validations: {
        word: wordIsValid,
        translation: translationIsValid
      },
      onDelete: word => {
        allWords.some((x, i) => x.id === word.id && (allWords.splice(i, 1), index = buildIndex(allWords), true));

        dataProvider.deleteWord(word.id).then(() => {
          wordsCount--;

          if (wordsCount === 0) {
            self.render();
          } else {
            word.onDeleted();
          }
        });
      },
      onUpdate: word => {
        if (word.id !== 0) {
          dataProvider.updateWord(word.id, word.word, word.translation).then(() => word.onUpdated(), onWordUpdateError);
        } else { // create
          dataProvider.createWord(word.word, word.translation).then(data => {
            hideDict();

            word.onUpdated();
            word.validationError(false)

            if (data.error === null && typeof data.id === 'number') {
              word.clearFields();

              // TODO: we can use binary search here
              let i = 0;
              allWords.some((x, j) => {
                if (x.word.toLowerCase() > word.word.toLowerCase()) return true;
                else return (j = i), false;
              });
              newWord = {id: data.id, word: word.word, translation: word.translation};
              allWords.splice(i, 0, newWord);
              index = buildIndex(allWords);

              wordsCount++;

              if (typeof selectedFilter === 'number') index[selectedFilter].active = false;
              selectedFilter = word.word;
              self.render();
            } else {
              console.error(data);
            }
          }, onWordUpdateError);
        }

        function onWordUpdateError(data) {
          if (data.error === 'validation') {
            word.onUpdated();
            word.validationError(true)
          } else {
            console.error(data); // TODO
          }
        }
      },
      onIndex(indexItem) {
        hideDict();

        if (typeof selectedFilter === 'number') index[selectedFilter].active = false;
        selectedFilter = indexItem.id;
        self.render();
      },
      onNewWordChange(text) {
        if (text.length === 0 || !wordIsValid(text)) {
          hideDict();
          return;
        }

        if (!dictIsShown) {
          element.changeDictState({
            isDict: true,
            isLoading: true
          });
          dictIsShown = true;
        }

        if (lastWordChangeTimeout !== null) clearTimeout(lastWordChangeTimeout);

        lastWordChangeTimeout = setTimeout(function() {
          lastWordChangeTimeout = null;

          if (!dictIsShown) return;

          dataProvider.getTranslations(text).then(data => {
            if (!dictIsShown) return;

            element.changeDictState({
              isDict: true,
              isLoading: false,
              translation: data
            });
          });
        }, 750);
      }
    });

    function hideDict() {
      dictIsShown = false;
      lastWordChangeTimeout && clearTimeout(lastWordChangeTimeout);
      element.changeDictState({isDict: false});
    }

    newWord = null;
  };

  function buildIndex(words) {
    // TODO: do it recursively, if some bucket have too much words

    let buckets = new Map();

    words.forEach(x => {
      let c = x.word[0].toLowerCase();

      let list = buckets.get(c);
      if (!list) buckets.set(c, list = []);

      list.push(x);
    });

    let result = [];

    for (let [key, value] of buckets) {
      key = key[0].toUpperCase() + key.slice(1);

      result.push({
        value: key,
        active: false
      });
    }

    result.sort(function(a, b) {
      if (a.value < b.value)   return -1;
      if (a.value === b.value) return 0;
                               return 1;
    });

    result.forEach((x, i) => x.id = i);

    return result;
  }
}
