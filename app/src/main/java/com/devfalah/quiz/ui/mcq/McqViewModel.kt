package com.devfalah.quiz.ui.mcq


import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.devfalah.quiz.data.model.Answer
import com.devfalah.quiz.data.repository.QuizRepositoryImp
import com.devfalah.quiz.data.model.QuizResponse
import com.devfalah.quiz.data.model.Quiz
import com.devfalah.quiz.data.service.WebRequest
import com.devfalah.quiz.utilities.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.logging.Handler
import kotlin.concurrent.schedule

class McqViewModel : ViewModel() {
    private val repository = QuizRepositoryImp(WebRequest().apiService)

    private var _requestState = MutableLiveData<State<QuizResponse>>(State.Loading)
    val requestState get() : LiveData<State<QuizResponse>> = _requestState
    private val questions = mutableListOf<Quiz>()


    private var _currentQuestionIndex = MutableLiveData<Int>(0)
    val currentQuestionIndex get() : LiveData<Int> = _currentQuestionIndex

    private var _allQuestionsSize = MutableLiveData<Int>(0)
    val allQuestionsSize get() : LiveData<Int> = _allQuestionsSize
    private val _currentQuestion = MutableLiveData<Quiz>()
    val currentQuestion get() : LiveData<Quiz> = _currentQuestion

    private val _currentQuestionAnswers = MutableLiveData<List<Answer>?>()
    val currentQuestionAnswers: LiveData<List<Answer>?> get() = _currentQuestionAnswers

    private val _score = MutableLiveData<Int>(0)
    val score: LiveData<Int> = _score


    init {
        getFifteenQuestions()
    }


    fun onClickAnswer(answer: Answer) {
        changeAnswerState(answer)
        _score.postValue(_score.value!!.plus(Constants.SCORE))

        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            goToNextQuestion()
        }

    }


    private fun getFifteenQuestions() {
        Observable.concat(
            getFiveQuestions(McqDifficulty.EASY).toObservable(),
            getFiveQuestions(McqDifficulty.MEDIUM).toObservable(),
            getFiveQuestions(McqDifficulty.HARD).toObservable()
        ).run {
            observeOnMainThread()
            subscribe(::onGetMCQsSuccess, ::onGetMCQsError)
        }
    }

    private fun onGetMCQsSuccess(state: State<QuizResponse>) {
        val result = requireNotNull(state.toData()?.questions)
        questions.addAll(result)
        questions.forEach { q -> Log.d("Sadeq", q.correctAnswer.toString()) }
        when (result.first()?.difficulty) {
            McqDifficulty.HARD.name.lowercase() -> {
                if (state is State.Success) {
                    _requestState.postValue(state)
                    setQuestion(questions.first())
                    _allQuestionsSize.postValue(questions.size)
                }
            }
        }
    }


    private fun setQuestion(quiz: Quiz) {
        _currentQuestion.postValue(quiz)
        setAnswer(quiz)

    }

    private fun setAnswer(quiz: Quiz) {
        val answers = quiz.incorrectAnswers?.map { it?.toAnswer(false) }
        _currentQuestionAnswers.postValue(
            answers?.plus(quiz.correctAnswer?.toAnswer(true))
                ?.shuffled() as List<Answer>
        )
    }

    private fun goToNextQuestion() {
        incrementCurrentQuestionIndex()
        if (questions.size > _currentQuestionIndex.value!!) {
            setQuestion(questions[_currentQuestionIndex.value!!])
        }
    }

    private fun incrementCurrentQuestionIndex() {
        _currentQuestionIndex.value = _currentQuestionIndex.value?.plus(1)!!
    }

    private fun onGetMCQsError(throwable: Throwable) =
        _requestState.postValue(State.Error(requireNotNull(throwable.message)))

    private fun getFiveQuestions(difficulty: McqDifficulty): Single<State<QuizResponse>> =
        repository.getQuizQuestions(difficulty)


    private fun changeAnswerState(answer: Answer) {
        _currentQuestionAnswers.postValue(_currentQuestionAnswers.value?.apply {
            if (answer.isCorrect) {
                answer.state = AnswerState.CORRECT
            } else {
                answer.state = AnswerState.INCORRECT
                this.filter { it.isCorrect }.forEach { it.state = AnswerState.CORRECT }
            }
        })
    }
}


